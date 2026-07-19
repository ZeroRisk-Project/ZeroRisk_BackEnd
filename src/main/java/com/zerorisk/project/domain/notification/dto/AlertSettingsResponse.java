package com.zerorisk.project.domain.notification.dto;

import com.zerorisk.project.domain.notification.entity.AlertSettings;

public record AlertSettingsResponse(
        boolean orderFilled,
        boolean commentAdded,
        boolean competition,
        boolean priceAlert,
        boolean inquiryAnswered) {

    public static AlertSettingsResponse from(AlertSettings settings) {
        return new AlertSettingsResponse(
                settings.getOrderFilled(),
                settings.getCommentAdded(),
                settings.getCompetition(),
                settings.getPriceAlert(),
                settings.getInquiryAnswered());
    }
}