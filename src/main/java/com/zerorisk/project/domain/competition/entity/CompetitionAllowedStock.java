package com.zerorisk.project.domain.competition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "COMPETITION_ALLOWED_STOCKS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionAllowedStock {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cas_seq")
    @SequenceGenerator(name = "cas_seq", sequenceName = "COMPETITION_ALLOWED_STOCKS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "COMPETITION_ID", nullable = false)
    private Long competitionId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Builder
    private CompetitionAllowedStock(Long competitionId, Long stockId) {
        this.competitionId = competitionId;
        this.stockId = stockId;
    }
}
