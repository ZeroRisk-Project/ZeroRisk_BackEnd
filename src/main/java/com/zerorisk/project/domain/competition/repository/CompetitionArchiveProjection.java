package com.zerorisk.project.domain.competition.repository;

import java.math.BigDecimal;

public interface CompetitionArchiveProjection {
    Integer getRank();

    String getNickname();

    BigDecimal getReturnRate();

    BigDecimal getTotalAsset();

    BigDecimal getPrizeAmount();
}