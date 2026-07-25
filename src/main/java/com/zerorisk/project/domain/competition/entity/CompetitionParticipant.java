package com.zerorisk.project.domain.competition.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "COMPETITION_PARTICIPANTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "competition_participants_seq")
    @SequenceGenerator(name = "competition_participants_seq", sequenceName = "COMPETITION_PARTICIPANTS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "COMPETITION_ID", nullable = false)
    private Long competitionId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "TOTAL_ASSET", nullable = false)
    private BigDecimal totalAsset;

    @Column(name = "RETURN_RATE", nullable = false, precision = 10, scale = 4)
    private BigDecimal returnRate;

    @Column(name = "RANK_POSITION")
    private Integer rankPosition;

    @Column(name = "JOINED_AT", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    private CompetitionParticipant(Long competitionId, Long userId, Long accountId, BigDecimal totalAsset) {
        this.competitionId = competitionId;
        this.userId = userId;
        this.accountId = accountId;
        this.totalAsset = totalAsset;
        this.returnRate = BigDecimal.ZERO;
        this.joinedAt = LocalDateTime.now();
    }
}