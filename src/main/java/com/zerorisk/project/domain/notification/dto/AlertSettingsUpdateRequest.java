package com.zerorisk.project.domain.notification.dto;

import jakarta.validation.constraints.NotNull;

public record AlertSettingsUpdateRequest(
        @NotNull Boolean orderFilled,
        @NotNull Boolean commentAdded,
        @NotNull Boolean competition,
        @NotNull Boolean priceAlert,
        @NotNull Boolean inquiryAnswered) {
}