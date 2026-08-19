package com.zerorisk.project.domain.profile.dto;

public record ProfileSettingsResponse(
        boolean showReturnRate,
        boolean showPortfolio,
        boolean showTrades,
        boolean showStats,
        boolean showCompetitions) {
}
