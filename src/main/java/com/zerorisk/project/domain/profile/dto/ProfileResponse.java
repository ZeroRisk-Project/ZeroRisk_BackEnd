package com.zerorisk.project.domain.profile.dto;

import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import com.zerorisk.project.domain.order.entity.OrderSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        Integer userLevel,
        Integer activityScore,
        LocalDateTime createdAt,
        long followerCount,
        long followingCount,
        boolean isFollowing,
        boolean isMe,
        long postCount,
        long commentCount,
        boolean returnRateVisible,
        ReturnRateInfo returnRate,
        boolean portfolioVisible,
        PortfolioSummary portfolio,
        boolean tradesVisible,
        List<TradeSummary> recentTrades,
        boolean statsVisible,
        StatsInfo stats,
        boolean competitionsVisible,
        List<ProfileCompetitionHistory> competitionHistory) {

    // targetReturnRate/viewerReturnRate는 비공개가 아니라 랭킹 미집계(스냅샷 없음)일 때도 null일 수 있음
    public record ReturnRateInfo(
            BigDecimal targetReturnRate,
            BigDecimal viewerReturnRate,
            LocalDate baseDate) {
    }

    // 절대 금액(cash, stockValue 등)은 의도적으로 넣지 않음 - 타인 프로필에는 비중(%)만 노출
    public record PortfolioSummary(
            BigDecimal cashRatio,
            BigDecimal stockRatio,
            List<StockWeightItem> stocks) {
    }

    public record StockWeightItem(
            String stockName,
            BigDecimal weight) {
    }

    // 수량/가격은 의도적으로 넣지 않음 - 타인 프로필에는 종목·매매구분·시간만 노출
    public record TradeSummary(
            String stockName,
            OrderSide side,
            LocalDateTime tradedAt) {
    }

    public record StatsInfo(
            BigDecimal winRate,
            BigDecimal avgReturnRate,
            long totalTradeCount) {
    }

    public record ProfileCompetitionHistory(
            Long competitionId,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal seedMoney,
            CompetitionStatus status,
            Integer rankPosition,
            BigDecimal returnRate,
            BigDecimal prizeAmount) {
    }
}
