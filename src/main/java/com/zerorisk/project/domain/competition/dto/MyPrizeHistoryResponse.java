package com.zerorisk.project.domain.competition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyPrizeHistoryResponse(
        Long competitionId,
        String competitionTitle,
        Integer rankPosition,
        BigDecimal prizeAmount,
        LocalDateTime paidAt) {
}