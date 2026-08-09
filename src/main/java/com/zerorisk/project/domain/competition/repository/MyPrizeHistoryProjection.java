package com.zerorisk.project.domain.competition.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MyPrizeHistoryProjection {
    Long getCompetitionId();
    String getCompetitionTitle();
    Integer getRankPosition();
    BigDecimal getPrizeAmount();
    LocalDateTime getPaidAt();
}
