package com.zerorisk.project.domain.systemnotice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SYSTEM_NOTICES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_notices_seq")
    @SequenceGenerator(name = "system_notices_seq", sequenceName = "SYSTEM_NOTICES_SEQ", allocationSize = 50)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", nullable = false, length = 20)
    private NoticeSeverity severity;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "MESSAGE", nullable = false, length = 1000)
    private String message;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SystemNotice(NoticeSeverity severity, String title, String message, Long createdBy) {
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.isActive = true;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
