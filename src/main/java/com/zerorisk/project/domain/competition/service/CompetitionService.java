package com.zerorisk.project.domain.competition.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
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
import com.zerorisk.project.domain.competition.repository.PrizeHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

                accountRepository.findByUserIdAndAccountType(userId, AccountType.BASIC)
                                .orElseThrow(() -> new CompetitionException(
                                                CompetitionErrorCode.BASIC_ACCOUNT_REQUIRED));

                Account competitionAccount = Account.builder()
                                .userId(userId)
                                .accountType(AccountType.COMPETITION)
                                .competitionId(competitionId)
                                .build();
                competitionAccount.addBalance(competition.getSeedMoney());
                accountRepository.save(competitionAccount);

                CompetitionParticipant participant = CompetitionParticipant.builder()
                                .competitionId(competitionId)
                                .userId(userId)
                                .accountId(competitionAccount.getId())
                                .totalAsset(competition.getSeedMoney())
                                .build();
                competitionParticipantRepository.save(participant);

                return new JoinCompetitionResponse(participant.getId(), competitionAccount.getId());
        }

        public boolean isJoined(Long competitionId, Long userId) {
                return competitionParticipantRepository.findByCompetitionIdAndUserId(competitionId, userId).isPresent();
        }

        @Transactional
        public void expelParticipant(Long competitionId, Long targetUserId) {
                CompetitionParticipant participant = competitionParticipantRepository
                                .findByCompetitionIdAndUserId(competitionId, targetUserId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                Account account = accountRepository.findById(participant.getAccountId())
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                account.zeroBalance();
                competitionParticipantRepository.delete(participant);
        }

        public List<CompetitionRankingResponse> getRankings(Long competitionId) {
                if (!competitionRepository.existsById(competitionId)) {
                        throw new CompetitionException(CompetitionErrorCode.NOT_FOUND);
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
                Competition competition = Competition.builder()
                                .title(request.title())
                                .description(request.description())
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

                return competition.getId();
        }

        @Transactional
        public void updateCompetition(Long competitionId, CompetitionUpdateRequest request) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() != CompetitionStatus.SCHEDULED) {
                        throw new CompetitionException(CompetitionErrorCode.CANNOT_UPDATE_ONGOING);
                }

                competition.updateInfo(
                                request.title(), request.description(),
                                request.startAt(), request.endAt(), request.isPublic(),
                                request.maxParticipants());
        }

        @Transactional
        public void deleteCompetition(Long competitionId) {
                Competition competition = competitionRepository.findById(competitionId)
                                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

                if (competition.getStatus() == CompetitionStatus.ONGOING) {
                        throw new CompetitionException(CompetitionErrorCode.CANNOT_DELETE_ONGOING);
                }

                competitionRepository.delete(competition);
        }

        private static final Map<Integer, BigDecimal> PRIZE_RULE = Map.of(
                        1, BigDecimal.valueOf(100_000),
                        2, BigDecimal.valueOf(50_000),
                        3, BigDecimal.valueOf(30_000));

        @Transactional
        public void distributePrizes(Long competitionId) {
                List<CompetitionRankingProjection> rankings = competitionRankingRepository
                                .findRankingsByCompetitionId(competitionId);

                for (CompetitionRankingProjection ranking : rankings) {
                        competitionParticipantRepository
                                        .findByCompetitionIdAndUserId(competitionId, ranking.getUserId())
                                        .ifPresent(participant -> participant.finalizeRank(ranking.getRankPosition()));

                        BigDecimal prizeAmount = PRIZE_RULE.get(ranking.getRankPosition());
                        if (prizeAmount == null) {
                                continue; // 4위 이하는 상금 없음
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
        }

        public List<MyPrizeHistoryResponse> getMyPrizeHistory(Long userId) {
                return prizeHistoryRepository.findByUserId(userId).stream()
                                .map(h -> new MyPrizeHistoryResponse(
                                                h.getCompetitionId(), h.getRankPosition(), h.getPrizeAmount(),
                                                h.getPaidAt()))
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