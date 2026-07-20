package com.zerorisk.project.domain.ranking.dto;

import java.math.BigDecimal;

// 내부 계산용 DTO. API 응답으로 직접 나가지 않음 (RankingResponse로 변환해서 나감)
public record AccountReturnRateRow(
        Long userId,
        String nickname,
        String userLevel,
        BigDecimal initialAsset,
        BigDecimal currentAsset) {
}