package com.zerorisk.project.domain.systemnotice.dto;

import com.zerorisk.project.domain.systemnotice.entity.NoticeSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemNoticeCreateRequest(
        @NotNull NoticeSeverity severity,
        @NotBlank String title,
        @NotBlank String message
) {
}
