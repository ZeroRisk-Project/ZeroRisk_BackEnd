package com.zerorisk.project.global.audit;

import java.time.LocalDateTime;

public record UserActivityLogResponse(
        String actionType,
        String detail,
        String ipAddress,
        LocalDateTime createdAt
) {
    public static UserActivityLogResponse from(UserActivityLog log) {
        return new UserActivityLogResponse(
                log.getActionType(), log.getDetail(), log.getIpAddress(), log.getCreatedAt()
        );
    }
}
