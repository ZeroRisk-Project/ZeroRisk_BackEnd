package com.zerorisk.project.domain.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionCreateRequest(
                @NotBlank String title,

                String description,

                @NotNull @Future LocalDateTime startAt,

                @NotNull @Future LocalDateTime endAt,

                @NotNull @DecimalMin(value = "1", message = "시드머니는 1원 이상이어야 합니다.") BigDecimal seedMoney,

                @NotNull Boolean isPublic) {
}