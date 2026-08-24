package com.zerorisk.project.domain.competition.dto;

import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime recruitStartAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        CompetitionStatus status,
        BigDecimal seedMoney,
        boolean joinable,
        Integer maxParticipants) {
    public static CompetitionDetailResponse from(Competition competition) {
        return new CompetitionDetailResponse(
                competition.getId(), competition.getTitle(), competition.getDescription(),
                competition.getRecruitStartAt(), competition.getStartAt(), competition.getEndAt(),
                competition.getStatus(), competition.getSeedMoney(), competition.isJoinable(),
                competition.getMaxParticipants());
    }
}