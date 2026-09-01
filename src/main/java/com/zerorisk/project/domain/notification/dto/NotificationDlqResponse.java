package com.zerorisk.project.domain.notification.dto;

import com.zerorisk.project.domain.notification.entity.NotificationDlq;
import com.zerorisk.project.domain.notification.entity.NotificationDlqStatus;
import com.zerorisk.project.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationDlqResponse(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        String failureReason,
        int retryCount,
        NotificationDlqStatus status,
        LocalDateTime createdAt) {

    public static NotificationDlqResponse from(NotificationDlq dlq) {
        return new NotificationDlqResponse(
                dlq.getId(),
                dlq.getUser().getId(),
                dlq.getType(),
                dlq.getTitle(),
                dlq.getMessage(),
                dlq.getFailureReason(),
                dlq.getRetryCount(),
                dlq.getStatus(),
                dlq.getCreatedAt());
    }
}
