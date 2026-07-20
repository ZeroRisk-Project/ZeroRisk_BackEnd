package com.zerorisk.project.domain.auth.dto;

public record KakaoUnlinkWebhookRequest(
        String app_id,
        String user_id,
        String referrer_type) {
}