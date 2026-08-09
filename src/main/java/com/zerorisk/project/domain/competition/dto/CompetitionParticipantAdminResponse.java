package com.zerorisk.project.domain.competition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionParticipantAdminResponse(
        Long userId,
        String nickname,
        String email,
        LocalDateTime joinedAt,
        BigDecimal returnRate,
        BigDecimal totalAsset
) {
}
