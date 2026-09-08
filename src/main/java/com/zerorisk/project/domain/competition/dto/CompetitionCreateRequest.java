package com.zerorisk.project.domain.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompetitionCreateRequest(
                @NotBlank String title,

                String description,

                // 모집 시작/마감일은 오늘 날짜로도 만들 수 있어야 하므로 @Future(현재 시각 이후) 대신
                // CompetitionService에서 날짜 단위(오늘 포함)로 검증한다.
                @NotNull LocalDateTime recruitStartAt,

                @NotNull LocalDateTime recruitEndAt,

                @NotNull @Future LocalDateTime startAt,

                @NotNull @Future LocalDateTime endAt,

                @NotNull @DecimalMin(value = "1", message = "시드머니는 1원 이상이어야 합니다.") BigDecimal seedMoney,

                @NotNull Boolean isPublic,

                List<Long> allowedStockIds,

                Integer maxParticipants) {
}