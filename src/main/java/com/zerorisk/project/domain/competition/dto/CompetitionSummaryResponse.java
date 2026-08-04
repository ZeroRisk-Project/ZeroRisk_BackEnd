package com.zerorisk.project.domain.competition.dto;

import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionSummaryResponse(
        Long id,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        CompetitionStatus status,
        BigDecimal seedMoney,
        Long participantCount) {
}