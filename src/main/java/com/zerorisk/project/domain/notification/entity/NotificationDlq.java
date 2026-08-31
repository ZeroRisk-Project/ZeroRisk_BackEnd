package com.zerorisk.project.domain.notification.entity;

import com.zerorisk.project.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "NOTIFICATION_DLQ")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDlq {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_dlq_seq")
    @SequenceGenerator(name = "notification_dlq_seq", sequenceName = "NOTIFICATION_DLQ_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "TARGET_URL", length = 500)
    private String targetUrl;

    @Lob
    @Column(name = "FAILURE_REASON")
    private String failureReason;

    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private NotificationDlqStatus status;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Builder
    private NotificationDlq(
            User user, NotificationType type, String title, String message,
            String targetUrl, String failureReason, int retryCount) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.status = NotificationDlqStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markResolved() {
        this.status = NotificationDlqStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markIgnored() {
        this.status = NotificationDlqStatus.IGNORED;
        this.resolvedAt = LocalDateTime.now();
    }
}
