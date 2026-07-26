package com.zerorisk.project.domain.competition.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRIZE_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrizeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prize_history_seq")
    @SequenceGenerator(name = "prize_history_seq", sequenceName = "PRIZE_HISTORY_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "COMPETITION_ID", nullable = false)
    private Long competitionId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "RANK_POSITION", nullable = false)
    private Integer rankPosition;

    @Column(name = "PRIZE_AMOUNT", nullable = false)
    private BigDecimal prizeAmount;

    @Column(name = "PAID_AT", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    @Builder
    private PrizeHistory(Long competitionId, Long userId, Long accountId, Integer rankPosition,
            BigDecimal prizeAmount) {
        this.competitionId = competitionId;
        this.userId = userId;
        this.accountId = accountId;
        this.rankPosition = rankPosition;
        this.prizeAmount = prizeAmount;
        this.paidAt = LocalDateTime.now();
    }
}