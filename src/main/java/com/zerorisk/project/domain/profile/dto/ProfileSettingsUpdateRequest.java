package com.zerorisk.project.domain.profile.dto;

import jakarta.validation.constraints.NotNull;

public record ProfileSettingsUpdateRequest(
        @NotNull Boolean showReturnRate,
        @NotNull Boolean showPortfolio,
        @NotNull Boolean showTrades,
        @NotNull Boolean showStats,
        @NotNull Boolean showCompetitions) {
}
