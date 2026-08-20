package com.zerorisk.project.global.audit;

public record UserActivityEvent(
        Long userId,
        String actionType,
        String detail,
        String ipAddress
) {
}
