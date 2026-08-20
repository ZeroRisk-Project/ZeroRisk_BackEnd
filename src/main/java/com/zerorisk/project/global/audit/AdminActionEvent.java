package com.zerorisk.project.global.audit;

public record AdminActionEvent(
        Long adminId,
        String actionType,
        String targetType,
        Long targetId,
        String detail,
        String ipAddress
) {
}
