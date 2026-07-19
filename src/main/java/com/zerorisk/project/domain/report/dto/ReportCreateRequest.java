package com.zerorisk.project.domain.report.dto;

import com.zerorisk.project.domain.report.entity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull TargetType targetType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 200) String reason) {
}