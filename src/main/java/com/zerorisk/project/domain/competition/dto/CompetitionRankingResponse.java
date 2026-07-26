package com.zerorisk.project.domain.competition.dto;

import java.math.BigDecimal;

public record CompetitionRankingResponse(
        Integer rank,
        Long userId,
        String nickname,
        BigDecimal returnRate,
        BigDecimal totalAsset) {
}