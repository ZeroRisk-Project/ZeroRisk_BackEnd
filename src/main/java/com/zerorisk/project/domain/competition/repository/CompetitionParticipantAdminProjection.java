package com.zerorisk.project.domain.competition.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompetitionParticipantAdminProjection {
    Long getUserId();
    String getNickname();
    String getEmail();
    LocalDateTime getJoinedAt();
    BigDecimal getReturnRate();
    BigDecimal getTotalAsset();
}
