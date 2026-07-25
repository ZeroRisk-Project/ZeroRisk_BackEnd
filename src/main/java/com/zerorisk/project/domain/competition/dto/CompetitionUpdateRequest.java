package com.zerorisk.project.domain.competition.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record CompetitionUpdateRequest(
        @NotBlank String title,

        String description,

        LocalDateTime startAt,

        LocalDateTime endAt,

        Boolean isPublic) {
}