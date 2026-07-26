package com.zerorisk.project.domain.competition.repository;

import java.math.BigDecimal;

public interface CompetitionRankingProjection {
    Integer getRankPosition();

    Long getUserId();

    String getNickname();

    BigDecimal getReturnRate();

    BigDecimal getTotalAsset();
}