package com.zerorisk.project.domain.profile.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.PrizeHistoryRepository;
import com.zerorisk.project.domain.competition.repository.ProfileCompetitionProjection;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.follow.repository.FollowRepository;
import com.zerorisk.project.domain.order.repository.TradeRepository;
import com.zerorisk.project.domain.order.service.TradeService;
import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
import com.zerorisk.project.domain.portfolio.service.HoldingService;
import com.zerorisk.project.domain.portfolio.service.PortfolioCompositionService;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.profile.dto.ProfileResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsUpdateRequest;
import com.zerorisk.project.domain.profile.entity.ProfileSettings;
import com.zerorisk.project.domain.profile.repository.ProfileSettingsRepository;
import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import com.zerorisk.project.domain.ranking.service.RankingService;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.MyRankingNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private static final int RECENT_TRADES_LIMIT = 5;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final PrizeHistoryRepository prizeHistoryRepository;
    private final ProfileSettingsRepository profileSettingsRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RankingService rankingService;
    private final AccountRepository accountRepository;
    private final HoldingService holdingService;
    private final PortfolioCompositionService portfolioCompositionService;
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    // findOrCreateSettings가 최초 조회 시 로우를 새로 INSERT할 수 있어 readOnly 트랜잭션을 쓰면 안 됨
    @Transactional
    public ProfileResponse getProfile(Long targetUserId, Long viewerId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        ProfileSettings settings = findOrCreateSettings(targetUserId);

        long followerCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        boolean isMe = viewerId != null && viewerId.equals(targetUserId);
        boolean isFollowing = viewerId != null && !isMe
                && followRepository.findByFollowerIdAndFollowingId(viewerId, targetUserId).isPresent();

        // 본인이 보거나, 공개 설정이 켜져 있으면 보여줌 (항목별 5개 모두 동일 규칙)
        boolean returnRateVisible = isMe || settings.getShowReturnRate();
        boolean portfolioVisible = isMe || settings.getShowPortfolio();
        boolean tradesVisible = isMe || settings.getShowTrades();
        boolean statsVisible = isMe || settings.getShowStats();
        boolean competitionsVisible = isMe || settings.getShowCompetitions();

        long postCount = postRepository.countByUser_IdAndIsDeletedFalse(targetUserId);
        long commentCount = commentRepository.countByUser_IdAndIsDeletedFalse(targetUserId);

        ProfileResponse.ReturnRateInfo returnRate = returnRateVisible
                ? resolveReturnRate(targetUserId, viewerId, isMe)
                : null;

        ProfileResponse.PortfolioSummary portfolio = portfolioVisible
                ? portfolioCompositionService.getCompositionSummaryForUserId(targetUserId)
                : null;

        List<ProfileResponse.TradeSummary> recentTrades = tradesVisible
                ? tradeService.getRecentTradesForUserId(targetUserId, RECENT_TRADES_LIMIT)
                : List.of();

        ProfileResponse.StatsInfo stats = statsVisible
                ? resolveStats(targetUserId)
                : null;

        return new ProfileResponse(
                target.getId(), target.getNickname(), target.getProfileImageUrl(),
                target.getUserLevel(), target.getActivityScore(), target.getCreatedAt(),
                followerCount, followingCount, isFollowing, isMe, postCount, commentCount,
                returnRateVisible, returnRate,
                portfolioVisible, portfolio,
                tradesVisible, recentTrades,
                statsVisible, stats,
                competitionsVisible, competitionsVisible ? getCompetitionHistory(targetUserId) : List.of());
    }

    // targetReturnRate/viewerReturnRate는 랭킹 스냅샷이 아직 없으면 null (비공개와는 다른 상태)
    private ProfileResponse.ReturnRateInfo resolveReturnRate(Long targetUserId, Long viewerId, boolean isMe) {
        RankingResponse targetRanking = fetchRanking(targetUserId);
        RankingResponse viewerRanking = (!isMe && viewerId != null) ? fetchRanking(viewerId) : null;

        return new ProfileResponse.ReturnRateInfo(
                targetRanking == null ? null : targetRanking.returnRate(),
                viewerRanking == null ? null : viewerRanking.returnRate(),
                targetRanking == null ? null : targetRanking.baseDate());
    }

    private RankingResponse fetchRanking(Long userId) {
        try {
            return rankingService.getMyRanking(RankingPeriod.ALL, userId);
        } catch (MyRankingNotFoundException e) {
            return null;
        }
    }

    // 실현손익 매칭 로직이 없어 "승률"은 현재 보유종목 기준 간이 지표로 계산한다.
    private ProfileResponse.StatsInfo resolveStats(Long targetUserId) {
        Optional<Account> accountOpt = accountRepository.findByUserIdAndAccountType(targetUserId, AccountType.BASIC);
        if (accountOpt.isEmpty()) {
            return new ProfileResponse.StatsInfo(null, null, 0);
        }

        Long accountId = accountOpt.get().getId();
        List<HoldingResponse> holdings = holdingService.getHoldings(accountId);
        long totalTradeCount = tradeRepository.countByAccountId(accountId);

        if (holdings.isEmpty()) {
            return new ProfileResponse.StatsInfo(null, null, totalTradeCount);
        }

        long winCount = holdings.stream()
                .filter(h -> h.profitRate().compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal winRate = BigDecimal.valueOf(winCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(holdings.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgReturnRate = holdings.stream()
                .map(HoldingResponse::profitRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(holdings.size()), 2, RoundingMode.HALF_UP);

        return new ProfileResponse.StatsInfo(winRate, avgReturnRate, totalTradeCount);
    }

    @Transactional
    public ProfileSettingsResponse getMySettings(Long userId) {
        ProfileSettings settings = findOrCreateSettings(userId);
        return new ProfileSettingsResponse(
                settings.getShowReturnRate(), settings.getShowPortfolio(), settings.getShowTrades(),
                settings.getShowStats(), settings.getShowCompetitions());
    }

    @Transactional
    public ProfileSettingsResponse updateMySettings(Long userId, ProfileSettingsUpdateRequest request) {
        ProfileSettings settings = findOrCreateSettings(userId);
        settings.update(
                request.showReturnRate(), request.showPortfolio(), request.showTrades(),
                request.showStats(), request.showCompetitions());
        return new ProfileSettingsResponse(
                settings.getShowReturnRate(), settings.getShowPortfolio(), settings.getShowTrades(),
                settings.getShowStats(), settings.getShowCompetitions());
    }

    private ProfileSettings findOrCreateSettings(Long userId) {
        return profileSettingsRepository.findByUserId(userId)
                .orElseGet(() -> profileSettingsRepository.save(
                        ProfileSettings.builder().userId(userId).build()));
    }

    private List<ProfileResponse.ProfileCompetitionHistory> getCompetitionHistory(Long userId) {
        List<ProfileCompetitionProjection> history = competitionParticipantRepository.findCompetitionHistoryByUserId(userId);

        Map<Long, BigDecimal> prizeMap = prizeHistoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(PrizeHistory::getCompetitionId, PrizeHistory::getPrizeAmount));

        return history.stream()
                .map(h -> new ProfileResponse.ProfileCompetitionHistory(
                        h.getCompetitionId(), h.getTitle(), h.getStartAt(), h.getEndAt(),
                        h.getSeedMoney(), h.getStatus(), h.getRankPosition(),
                        h.getReturnRate(), prizeMap.get(h.getCompetitionId())))
                .toList();
    }
}
