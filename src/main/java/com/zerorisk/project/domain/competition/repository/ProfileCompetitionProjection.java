package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProfileCompetitionProjection {
    Long getCompetitionId();
    String getTitle();
    LocalDateTime getStartAt();
    LocalDateTime getEndAt();
    BigDecimal getSeedMoney();
    CompetitionStatus getStatus();
    Integer getRankPosition();
    BigDecimal getReturnRate();
}
