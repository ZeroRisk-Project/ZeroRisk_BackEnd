package com.zerorisk.project.global.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ADMIN_ACTION_LOGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_action_logs_seq")
    @SequenceGenerator(name = "admin_action_logs_seq", sequenceName = "ADMIN_ACTION_LOGS_SEQ", allocationSize = 50)
    private Long id;

    @Column(name = "ADMIN_ID", nullable = false)
    private Long adminId;

    @Column(name = "ACTION_TYPE", nullable = false, length = 30)
    private String actionType;

    @Column(name = "TARGET_TYPE", nullable = false, length = 30)
    private String targetType;

    @Column(name = "TARGET_ID", nullable = false)
    private Long targetId;

    @Column(name = "DETAIL", length = 500)
    private String detail;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AdminActionLog(Long adminId, String actionType, String targetType, Long targetId, String detail) {
        this.adminId = adminId;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }
}
