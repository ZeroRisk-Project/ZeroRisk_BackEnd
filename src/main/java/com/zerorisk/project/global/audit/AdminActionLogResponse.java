package com.zerorisk.project.global.audit;

import java.time.LocalDateTime;

public record AdminActionLogResponse(
        Long id,
        String adminNickname,
        String actionType,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt) {
}
