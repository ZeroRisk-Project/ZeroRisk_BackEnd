package com.zerorisk.project.domain.competition.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "COMPETITIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "competitions_seq")
    @SequenceGenerator(name = "competitions_seq", sequenceName = "COMPETITIONS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "RECRUIT_START_AT")
    private LocalDateTime recruitStartAt;

    @Column(name = "START_AT", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "END_AT", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private CompetitionStatus status;

    @Column(name = "SEED_MONEY", nullable = false)
    private BigDecimal seedMoney;

    @Column(name = "IS_PUBLIC", nullable = false)
    private Boolean isPublic;

    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;

    @Column(name = "MAX_PARTICIPANTS")
    private Integer maxParticipants;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Competition(String title, String description, LocalDateTime recruitStartAt, LocalDateTime startAt,
            LocalDateTime endAt, BigDecimal seedMoney, Boolean isPublic, Long createdBy, Integer maxParticipants) {
        this.title = title;
        this.description = description;
        this.recruitStartAt = recruitStartAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = CompetitionStatus.SCHEDULED;
        this.seedMoney = seedMoney;
        this.isPublic = isPublic;
        this.createdBy = createdBy;
        this.maxParticipants = maxParticipants;
        this.createdAt = LocalDateTime.now();
    }

    public void startCompetition() {
        this.status = CompetitionStatus.ONGOING;
    }

    public void startCalculating() {
        this.status = CompetitionStatus.CALCULATING;
    }

    public void endCompetition() {
        this.status = CompetitionStatus.ENDED;
    }

    public boolean isJoinable() {
        if (this.status != CompetitionStatus.SCHEDULED) {
            return false;
        }
        return this.recruitStartAt == null || !LocalDateTime.now().isBefore(this.recruitStartAt);
    }

    public void updateInfo(String title, String description, LocalDateTime recruitStartAt, LocalDateTime startAt,
            LocalDateTime endAt, Boolean isPublic, Integer maxParticipants) {
        this.title = title;
        this.description = description;
        if (recruitStartAt != null)
            this.recruitStartAt = recruitStartAt;
        if (startAt != null)
            this.startAt = startAt;
        if (endAt != null)
            this.endAt = endAt;
        this.isPublic = isPublic;
        this.maxParticipants = maxParticipants;
    }
}