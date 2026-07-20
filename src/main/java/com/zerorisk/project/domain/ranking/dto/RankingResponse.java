package com.zerorisk.project.domain.ranking.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 주의: 총자산(currentAsset, initialAsset) 필드는 절대 추가하지 말 것.
// 전체 랭킹은 수익률(%)만 노출한다는 기획 원칙을 DTO 레벨에서 강제함.
public record RankingResponse(
        int rank,
        Long userId,
        String nickname,
        String userLevel,
        BigDecimal returnRate) {

    private static final int SCALE = 2;

    public static RankingResponse of(int rank, AccountReturnRateRow row) {
        BigDecimal returnRate = row.currentAsset()
                .subtract(row.initialAsset())
                .divide(row.initialAsset(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new RankingResponse(rank, row.userId(), row.nickname(), row.userLevel(), returnRate);
    }
}