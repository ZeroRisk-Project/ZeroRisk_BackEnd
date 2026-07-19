package com.zerorisk.project.domain.notification.dto;

import com.zerorisk.project.domain.notification.entity.Notification;
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
}