package com.zerorisk.project.domain.competition.dto;

import com.zerorisk.project.domain.competition.entity.FailedRecalculation;
import java.time.LocalDateTime;

public record FailedRecalculationResponse(
        Long id,
        Long participantId,
        String failureReason,
        Integer retryCount,
        boolean resolved,
        LocalDateTime createdAt) {

    public static FailedRecalculationResponse from(FailedRecalculation entity) {
        return new FailedRecalculationResponse(
                entity.getId(),
                entity.getParticipantId(),
                entity.getFailureReason(),
                entity.getRetryCount(),
                entity.isResolved(),
                entity.getCreatedAt());
    }
}
