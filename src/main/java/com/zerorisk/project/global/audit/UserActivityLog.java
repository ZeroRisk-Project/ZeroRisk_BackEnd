package com.zerorisk.project.global.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USER_ACTIVITY_LOGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_activity_logs_seq")
    @SequenceGenerator(name = "user_activity_logs_seq", sequenceName = "USER_ACTIVITY_LOGS_SEQ", allocationSize = 50)
    private Long id;

    @Column(name = "USER_ID", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "ACTION_TYPE", nullable = false, length = 30, updatable = false)
    private String actionType;

    @Column(name = "DETAIL", length = 500, updatable = false)
    private String detail;

    @Column(name = "IP_ADDRESS", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserActivityLog(Long userId, String actionType, String detail, String ipAddress) {
        this.userId = userId;
        this.actionType = actionType;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }
}
