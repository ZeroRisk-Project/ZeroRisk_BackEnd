package com.zerorisk.project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record UserSuspendRequest(
        LocalDateTime suspendedUntil,
        @NotBlank String reason) {
}