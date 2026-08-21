package com.zerorisk.project.domain.systemnotice.dto;

import com.zerorisk.project.domain.systemnotice.entity.SystemNotice;
import java.time.LocalDateTime;

public record SystemNoticeResponse(
        Long id, String severity, String title, String message, boolean isActive, LocalDateTime createdAt
) {
    public static SystemNoticeResponse from(SystemNotice n) {
        return new SystemNoticeResponse(n.getId(), n.getSeverity().name(), n.getTitle(), n.getMessage(), n.getIsActive(), n.getCreatedAt());
    }
}
