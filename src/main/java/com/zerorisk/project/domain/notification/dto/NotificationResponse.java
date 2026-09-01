package com.zerorisk.project.domain.notification.dto;

import com.zerorisk.project.domain.notification.entity.Notification;
import com.zerorisk.project.domain.notification.entity.NotificationDlq;
import com.zerorisk.project.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean isRead,
        String targetUrl,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getIsRead(),
                notification.getTargetUrl(),
                notification.getCreatedAt());
    }

    // DLQ 재발송 시, DLQ 항목 자체의 정보로 SSE 페이로드를 재구성 (원본 id는 DLQ 항목 id를 그대로 씀)
    public static NotificationResponse from(NotificationDlq dlq) {
        return new NotificationResponse(
                dlq.getId(),
                dlq.getType(),
                dlq.getTitle(),
                dlq.getMessage(),
                false,
                dlq.getTargetUrl(),
                dlq.getCreatedAt());
    }
}