package com.zerorisk.project.domain.competition.repository;

import java.math.BigDecimal;

public interface ProfileCompetitionProjection {
    Long getCompetitionId();
    String getTitle();
    Integer getRankPosition();
    BigDecimal getReturnRate();
}
