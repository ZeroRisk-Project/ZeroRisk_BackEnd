package com.zerorisk.project.domain.competition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionArchiveResponse(
        Long competitionId,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<ArchiveEntry> results) {
    public record ArchiveEntry(
            Integer rank,
            String nickname,
            BigDecimal returnRate,
            BigDecimal totalAsset,
            BigDecimal prizeAmount) {
    }
}