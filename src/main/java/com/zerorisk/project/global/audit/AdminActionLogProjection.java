package com.zerorisk.project.global.audit;

import java.time.LocalDateTime;

public interface AdminActionLogProjection {
    Long getId();
    String getAdminNickname();
    String getActionType();
    String getTargetType();
    Long getTargetId();
    String getDetail();
    LocalDateTime getCreatedAt();
}
