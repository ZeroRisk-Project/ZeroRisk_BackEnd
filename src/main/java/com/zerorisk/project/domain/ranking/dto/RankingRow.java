package com.zerorisk.project.domain.ranking.dto;

import java.math.BigDecimal;

// 내부 계산용 DTO - SQL의 RANK()가 이미 순위/수익률까지 계산해서 반환한 원본 행 (API 응답으로 직접 나가지 않음)
public record RankingRow(
        int rankPosition,
        Long userId,
        String nickname,
        String userLevel,
        BigDecimal returnRate) {
}
