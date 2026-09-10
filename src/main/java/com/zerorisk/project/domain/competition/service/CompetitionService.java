package com.zerorisk.project.domain.competition.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.chat.cache.CompetitionParticipantCache;
import com.zerorisk.project.domain.competition.dto.CompetitionArchiveResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionCreateRequest;
import com.zerorisk.project.domain.competition.dto.CompetitionDetailResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionRankingResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionSummaryResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionUpdateRequest;
import com.zerorisk.project.domain.competition.dto.JoinCompetitionResponse;
import com.zerorisk.project.domain.competition.dto.MyJoinedCompetitionsResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionParticipantAdminResponse;
import com.zerorisk.project.domain.competition.dto.MyPrizeHistoryResponse;
import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionAllowedStock;
import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import com.zerorisk.project.domain.competition.exception.CompetitionErrorCode;
import com.zerorisk.project.domain.competition.exception.CompetitionException;
import com.zerorisk.project.domain.competition.repository.CompetitionAllowedStockRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantAdminProjection;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantCountProjection;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionRankingProjection;
import com.zerorisk.project.domain.competition.repository.CompetitionRankingRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionRepository;
import com.zerorisk.project.domain.competition.repository.FailedRecalculationRepository;
import com.zerorisk.project.domain.competition.repository.PrizeHistoryRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserRole;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionService {

        private final CompetitionRepository competitionRepository;
        private final CompetitionParticipantRepository competitionParticipantRepository;
        private final AccountRepository accountRepository;
        private final CompetitionRankingRepository competitionRankingRepository;
        private final PrizeHistoryRepository prizeHistoryRepository;
        private final CompetitionAllowedStockRepository competitionAllowedStockRepository;
        private final CompetitionAssetService competitionAssetService;
        private final FailedRecalculationService failedRecalculationService;
        private final FailedRecalculationRepository failedRecalculationRepository;
        private final AdminActionLogger adminActionLogger;
        private final UserActivityLogger userActivityLogger;
        private final CompetitionParticipantCache competitionParticipantCache;
        private final RecalculationMetrics recalculationMetrics;
        private final UserRepository userRepository;

        public Page<CompetitionSummaryResponse> getCompetitions(Pageable pageable) {
                Page<Competition> competitions = competitionRepository.findByIsPublicTrue(pageable);

                List<Long> competitionIds = competitions.getContent().stream()
                                .map(Competition::getId)
                                .toList();

                Map<Long, Long> participantCountMap = competitionIds.isEmpty()
                                ? Map.of()
                                : competitionParticipantRepository.countByCompetitionIds(competitionIds).stream()
                                                .collect(Collectors.toMap(
                                                                CompetitionParticipantCountProjection::getCompetitionId,
                                                                CompetitionParticipantCountProjection::getCount));

                return competitions.map(competition -> new CompetitionSummaryResponse(
                                competition.getId(), competition.getTitle(),
                                competition.getStartAt(), competition.getEndAt(),
                                competition.getStatus(), competition.getSeedMoney(),
                                participantCountMap.getOrDefault(competition.getId(), 0L),
                                competition.getMaxParticipants()));
        }

        public CompetitionDetailResponse getCompetitionDetail(Long competitionId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));
                return CompetitionDetailResponse.from(competition);
        }

        @Transactional
        public JoinCompetitionResponse joinCompetition(Long competitionId, Long userId) {
                Competition competition = competitionRepository.findByIdForUpdate(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (!competition.isJoinable()) {
                        throw new CompetitionException(CompetitionErrorCode.NOT_JOINABLE);
                }

                if (competition.getMaxParticipants() != null) {
                        long currentCount = competitionParticipantRepository.countByCompetitionId(competitionId);
                        if (currentCount >= competition.getMaxParticipants()) {
                                throw new CompetitionException(CompetitionErrorCode.CAPACITY_EXCEEDED);
                        }
                }

                if (competitionParticipantRepository.findByCompetitionIdAndUserId(competitionId, userId).isPresent()) {
                        throw new CompetitionException(CompetitionErrorCode.ALREADY_JOINED);
                }

                long activeParticipationCount = competitionParticipantRepository.countByUserIdAndCompetitionStatusIn(
                                userId, List.of(CompetitionStatus.SCHEDULED, CompetitionStatus.ONGOING,
                                                CompetitionStatus.CALCULATING));
                if (activeParticipationCount >= 2) {
                        throw new CompetitionException(CompetitionErrorCode.MAX_PARTICIPATION_EXCEEDED);
                }

                if (accountRepository.findByUserIdAndAccountType(userId, AccountType.BASIC).isEmpty()) {
                        // 일반 회원은 오픈뱅킹 인증(또는 연습용 크레딧 수령)을 거쳐야 BASIC 계좌가 생기지만,
                        // 관리자는 그 온보딩 과정을 거치지 않으므로 대회 참가 시점에 빈 BASIC 계좌를 만들어 준다.
                        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
                        if (user.getUserRole() != UserRole.ADMIN) {
                                throw new CompetitionException(CompetitionErrorCode.BASIC_ACCOUNT_REQUIRED);
                        }
                        accountRepository.save(Account.builder()
                                        .userId(userId)
                                        .accountType(AccountType.BASIC)
                                        .build());
                }

                // 대회 시작 전까지는 비활성 계좌 - 시드머니도 대회 시작 시점(startCompetition)에 지급
                Account competitionAccount = Account.builder()
                                .userId(userId)
                                .accountType(AccountType.COMPETITION)
                                .competitionId(competitionId)
                                .build();
                competitionAccount.deactivate();
                accountRepository.save(competitionAccount);

                CompetitionParticipant participant = CompetitionParticipant.builder()
                                .competitionId(competitionId)
                                .userId(userId)
                                .accountId(competitionAccount.getId())
                                .totalAsset(competition.getSeedMoney())
                                .build();
                competitionParticipantRepository.save(participant);
                competitionParticipantCache.add(competitionId, userId);

                userActivityLogger.log(userId, "JOIN_COMPETITION", "[" + competition.getTitle() + "] 대회 참가");

                return new JoinCompetitionResponse(participant.getId(), competitionAccount.getId());
        }

        public boolean isJoined(Long competitionId, Long userId) {
                return competitionParticipantRepository.findByCompetitionIdAndUserId(competitionId, userId).isPresent();
        }

        // 스케줄러 전용 - SCHEDULED -> ONGOING 전환과 동시에, 참가자 전원의 계좌를 활성화하고 시드머니를 지급한다.
        @Transactional
        public void startCompetition(Long competitionId) {
                Competition competition = competitionRepository.findByIdForUpdate(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() != CompetitionStatus.SCHEDULED) {
                        return; // 이미 시작 처리된 대회면 아무것도 안 함 (스케줄러 중복 실행 시 시드머니 중복 지급 방지)
                }

                competition.startCompetition();

                List<CompetitionParticipant> participants = competitionParticipantRepository
                                .findByCompetitionId(competitionId);
                for (CompetitionParticipant participant : participants) {
                        Account account = accountRepository.findById(participant.getAccountId())
                                        .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));
                        account.activate();
                        account.addSeedMoney(competition.getSeedMoney());
                }
        }

        // 관리자 강제 퇴장 - 대회 진행 중(ONGOING) 언제든 가능하므로, 계좌가 이미 활성화되어
        // 실제 거래가 있었을 수 있다. 계좌는 삭제하지 않고 잔액만 0으로 정리한다.
        @Transactional
        public void expelParticipant(Long competitionId, Long targetUserId, Long adminId) {
                CompetitionParticipant participant = competitionParticipantRepository
                                .findByCompetitionIdAndUserId(competitionId, targetUserId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                Account account = accountRepository.findById(participant.getAccountId())
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                account.zeroBalance();
                competitionParticipantRepository.delete(participant);
                competitionParticipantCache.remove(competitionId, targetUserId);

                adminActionLogger.log(adminId, "EXPEL", "COMPETITION_PARTICIPANT", targetUserId,
                                String.format("대회 #%d 참가자(유저 #%d) 강제 퇴장", competitionId, targetUserId));
        }

        // 본인 요청으로 참가 취소 - 대회가 아직 시작 전(SCHEDULED)일 때만 허용.
        // 이 시점의 계좌는 항상 비활성 + 시드머니 미지급 상태이고(OrderService도 비활성 계좌 거래를 막음)
        // 실제 거래 자체가 원천적으로 불가능했으므로 계좌를 완전히 삭제해도 안전하다.
        @Transactional
        public void cancelParticipation(Long userId, Long competitionId) {
                Competition competition = competitionRepository.findByIdForUpdate(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() != CompetitionStatus.SCHEDULED) {
                        throw new CompetitionException(CompetitionErrorCode.CANNOT_CANCEL_AFTER_START);
                }

                CompetitionParticipant participant = competitionParticipantRepository
                                .findByCompetitionIdAndUserId(competitionId, userId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                competitionParticipantRepository.delete(participant);
                accountRepository.deleteById(participant.getAccountId());
                competitionParticipantCache.remove(competitionId, userId);
        }

        public List<CompetitionRankingResponse> getRankings(Long competitionId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                // 모집중(아직 시작 전)에는 수익률 랭킹이 존재하지 않으므로 참여한 순서대로 보여준다.
                if (competition.getStatus() == CompetitionStatus.SCHEDULED) {
                        List<CompetitionParticipantAdminProjection> participants = competitionParticipantRepository
                                        .findParticipantsWithUserInfo(competitionId);
                        List<CompetitionRankingResponse> rankings = new ArrayList<>();
                        for (int i = 0; i < participants.size(); i++) {
                                CompetitionParticipantAdminProjection p = participants.get(i);
                                rankings.add(new CompetitionRankingResponse(
                                                i + 1, p.getUserId(), p.getNickname(),
                                                p.getReturnRate(), p.getTotalAsset()));
                        }
                        return rankings;
                }

                return competitionRankingRepository.findRankingsByCompetitionId(competitionId).stream()
                                .map(p -> new CompetitionRankingResponse(
                                                p.getRankPosition(), p.getUserId(), p.getNickname(),
                                                p.getReturnRate(), p.getTotalAsset()))
                                .toList();
        }

        public List<CompetitionParticipantAdminResponse> getParticipantsForAdmin(Long competitionId) {
                if (!competitionRepository.existsById(competitionId)) {
                        throw new CompetitionException(CompetitionErrorCode.NOT_FOUND);
                }
                return competitionParticipantRepository.findParticipantsWithUserInfo(competitionId).stream()
                                .map(p -> new CompetitionParticipantAdminResponse(
                                                p.getUserId(), p.getNickname(), p.getEmail(),
                                                p.getJoinedAt(), p.getReturnRate(), p.getTotalAsset()))
                                .toList();
        }

        @Transactional
        public Long createCompetition(CompetitionCreateRequest request, Long adminUserId) {
                if (!request.recruitStartAt().isBefore(request.recruitEndAt())
                                || !request.recruitEndAt().isBefore(request.startAt())) {
                        throw new CompetitionException(CompetitionErrorCode.INVALID_RECRUIT_PERIOD);
                }

                // 모집일은 오늘 날짜로도 만들 수 있어야 하므로, 시각까지는 안 보고 날짜 단위로만 과거 여부를 검증한다.
                LocalDate today = LocalDate.now();
                if (request.recruitStartAt().toLocalDate().isBefore(today)
                                || request.recruitEndAt().toLocalDate().isBefore(today)) {
                        throw new CompetitionException(CompetitionErrorCode.INVALID_RECRUIT_PERIOD);
                }

                Competition competition = Competition.builder()
                                .title(request.title())
                                .description(request.description())
                                .recruitStartAt(request.recruitStartAt())
                                .recruitEndAt(request.recruitEndAt())
                                .startAt(request.startAt())
                                .endAt(request.endAt())
                                .seedMoney(request.seedMoney())
                                .isPublic(request.isPublic())
                                .createdBy(adminUserId)
                                .maxParticipants(request.maxParticipants())
                                .build();

                competitionRepository.save(competition);

                if (request.allowedStockIds() != null) {
                        for (Long stockId : request.allowedStockIds()) {
                                competitionAllowedStockRepository.save(
                                                CompetitionAllowedStock.builder()
                                                                .competitionId(competition.getId())
                                                                .stockId(stockId)
                                                                .build());
                        }
                }

                adminActionLogger.log(adminUserId, "CREATE", "COMPETITION", competition.getId(),
                                String.format("[%s] 대회 생성", competition.getTitle()));

                return competition.getId();
        }

        @Transactional
        public void updateCompetition(Long competitionId, CompetitionUpdateRequest request, Long adminUserId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() != CompetitionStatus.SCHEDULED) {
                        throw new CompetitionException(CompetitionErrorCode.CANNOT_UPDATE_ONGOING);
                }

                competition.updateInfo(
                                request.title(), request.description(), request.recruitStartAt(), request.recruitEndAt(),
                                request.startAt(), request.endAt(), request.isPublic(),
                                request.maxParticipants());

                adminActionLogger.log(adminUserId, "UPDATE", "COMPETITION", competitionId,
                                String.format("[%s] 대회 정보 수정", competition.getTitle()));
        }

        @Transactional
        public void deleteCompetition(Long competitionId, Long adminUserId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() == CompetitionStatus.ONGOING
                                || competition.getStatus() == CompetitionStatus.CALCULATING) {
                        throw new CompetitionException(CompetitionErrorCode.CANNOT_DELETE_ONGOING);
                }

                adminActionLogger.log(adminUserId, "DELETE", "COMPETITION", competitionId,
                                String.format("[%s] 대회 삭제", competition.getTitle()));

                competitionRepository.delete(competition);
        }

        private static final Map<Integer, BigDecimal> PRIZE_RULE = Map.of(
                        1, BigDecimal.valueOf(100_000),
                        2, BigDecimal.valueOf(50_000),
                        3, BigDecimal.valueOf(30_000));

        @Transactional
        public void distributePrizes(Long competitionId) {
                Competition competition = competitionRepository.findByIdForUpdate(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() == CompetitionStatus.ENDED) {
                        return; // 이미 처리 완료된 대회면 아무것도 안 함 (스케줄러 중복 실행 등으로 인한 상금 중복 지급 방지)
                }

                // 상금 지급 직전, 참가자 전원의 자산을 최신 시세 기준으로 딱 한 번 재평가한다.
                List<CompetitionParticipant> participants = competitionParticipantRepository
                                .findByCompetitionId(competitionId);

                // 참가자마다 recalculate()를 부르면 같은 종목을 참가자 수만큼 중복으로 KIS 조회하게 되므로,
                // 필요한 종목 시세를 먼저 한 번에 캐시해둔다(PortfolioSnapshotService와 동일한 패턴).
                Map<Long, BigDecimal> priceByStockId = competitionAssetService.prefetchPrices(participants);

                for (CompetitionParticipant participant : participants) {
                        long startedAt = System.currentTimeMillis();
                        try {
                                if (competitionAssetService.hasAllPricesCached(participant, priceByStockId)) {
                                        competitionAssetService.recalculateFromCache(participant, priceByStockId);
                                } else {
                                        // 일괄 조회에 실패한 종목을 보유한 참가자만 기존 경로로 - 재시도(@Retryable) 끝까지
                                        // 실패해도 @Recover가 흡수하므로 여기서 예외가 안 올라옴
                                        competitionAssetService.recalculate(participant);
                                }
                        } catch (Exception e) {
                                // @Recover는 IllegalStateException(KIS 조회 실패)만 흡수한다. 종목/계좌 조회 실패 등
                                // 그 외 예외는 그대로 던져지면 대회 전체 상금 지급 트랜잭션이 롤백되어 이 대회가
                                // 영원히 CALCULATING 상태로 멈추므로, 여기서 이 참가자만 DLQ로 격리하고 계속 진행한다.
                                log.warn("참가자 자산 재평가 실패(예상 밖 예외) - participantId: {}, reason: {}",
                                                participant.getId(), e.getMessage(), e);
                                failedRecalculationService.saveFailure(participant.getId(), e.getMessage());
                        }
                        recalculationMetrics.recordCompletionTime(System.currentTimeMillis() - startedAt);
                }

                List<CompetitionRankingProjection> rankings = competitionRankingRepository
                                .findRankingsByCompetitionId(competitionId);

                // RANK() 기준이라 동순위는 같은 rankPosition을 여러 명이 공유한다.
                // 순위별 상금은 "그 순위 전체가 나눠 갖는 총액"이므로, 동순위 인원수로 나눠서 지급한다.
                Map<Integer, Long> participantCountByRank = rankings.stream()
                                .collect(Collectors.groupingBy(CompetitionRankingProjection::getRankPosition, Collectors.counting()));

                for (CompetitionRankingProjection ranking : rankings) {
                        var participantOpt = competitionParticipantRepository
                                        .findByCompetitionIdAndUserId(competitionId, ranking.getUserId());
                        participantOpt.ifPresent(participant -> participant.finalizeRank(ranking.getRankPosition()));

                        BigDecimal tierPrize = PRIZE_RULE.get(ranking.getRankPosition());
                        if (tierPrize == null) {
                                continue; // 4위 이하는 상금 없음
                        }

                        long tieCount = participantCountByRank.getOrDefault(ranking.getRankPosition(), 1L);
                        // 원 단위로 내림 처리 - 나눠서 남는 잔돈은 지급하지 않는다(대회 총 지급액이 정해진 금액을 넘지 않도록).
                        BigDecimal prizeAmount = tierPrize.divide(BigDecimal.valueOf(tieCount), 0, RoundingMode.DOWN);
                        if (prizeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                                continue; // 동순위 인원이 너무 많아 1인당 지급액이 0원이 되는 경우
                        }

                        // 방금 위에서 재평가가 실패해 DLQ로 격리된 참가자라면, 화면에 보이는 자산/수익률이
                        // 최신 시세 기준이 아닌 이전 값(stale)일 수 있다. 신뢰할 수 없는 값으로 상금을
                        // 확정 지급하지 않고 관리자가 FAILED_RECALCULATIONS를 수동 재처리한 뒤 처리하도록 보류한다.
                        boolean hasUnresolvedRecalcFailure = participantOpt
                                        .map(p -> failedRecalculationRepository.existsByParticipantIdAndResolvedFalse(p.getId()))
                                        .orElse(false);
                        if (hasUnresolvedRecalcFailure) {
                                log.warn("상금 지급 보류 - userId: {}, competitionId: {}, reason: 자산 재평가 미해결 건 존재(최신 자산가치 신뢰 불가)",
                                                ranking.getUserId(), competitionId);
                                continue;
                        }

                        Account basicAccount = accountRepository
                                        .findByUserIdAndAccountType(ranking.getUserId(), AccountType.BASIC)
                                        .orElse(null);
                        if (basicAccount == null) {
                                log.warn("상금 지급 실패 - userId: {}, reason: BASIC 계좌 없음", ranking.getUserId());
                                continue;
                        }

                        basicAccount.addBalance(prizeAmount);

                        PrizeHistory history = PrizeHistory.builder()
                                        .competitionId(competitionId)
                                        .userId(ranking.getUserId())
                                        .accountId(basicAccount.getId())
                                        .rankPosition(ranking.getRankPosition())
                                        .prizeAmount(prizeAmount)
                                        .build();
                        prizeHistoryRepository.save(history);

                        log.info("상금 지급 완료 - userId: {}, rank: {}, amount: {}", ranking.getUserId(),
                                        ranking.getRankPosition(),
                                        prizeAmount);
                }

                // 대회가 끝나면 대회 전용 계좌는 더 이상 쓰이지 않으므로 비활성화해 숨긴다(마이페이지/헤더 계좌 목록에서 제외).
                for (CompetitionParticipant participant : participants) {
                        accountRepository.findById(participant.getAccountId())
                                        .ifPresent(Account::deactivate);
                }

                competition.endCompetition();
                competitionParticipantCache.evictAll(competitionId);
        }

        public List<MyPrizeHistoryResponse> getMyPrizeHistory(Long userId) {
                return prizeHistoryRepository.findByUserIdWithCompetitionTitle(userId).stream()
                                .map(p -> new MyPrizeHistoryResponse(
                                                p.getCompetitionId(), p.getCompetitionTitle(),
                                                p.getRankPosition(), p.getPrizeAmount(), p.getPaidAt()))
                                .toList();
        }

        public CompetitionArchiveResponse getArchive(Long competitionId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() != CompetitionStatus.ENDED) {
                        throw new CompetitionException(CompetitionErrorCode.ARCHIVE_NOT_READY);
                }

                List<CompetitionArchiveResponse.ArchiveEntry> entries = competitionParticipantRepository
                                .findArchiveByCompetitionId(competitionId).stream()
                                .map(r -> new CompetitionArchiveResponse.ArchiveEntry(
                                                r.getRank(), r.getNickname(), r.getReturnRate(), r.getTotalAsset(),
                                                r.getPrizeAmount()))
                                .toList();

                return new CompetitionArchiveResponse(
                                competition.getId(), competition.getTitle(),
                                competition.getStartAt(), competition.getEndAt(), entries);
        }

        public MyJoinedCompetitionsResponse getMyJoinedCompetitions(Long userId) {
                List<Long> ids = competitionParticipantRepository.findByUserId(userId).stream()
                                .map(CompetitionParticipant::getCompetitionId)
                                .toList();
                return new MyJoinedCompetitionsResponse(ids);
        }

}