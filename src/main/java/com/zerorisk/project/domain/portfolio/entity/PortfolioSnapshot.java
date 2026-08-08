package com.zerorisk.project.domain.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PORTFOLIO_SNAPSHOTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portfolio_snapshots_seq")
    @SequenceGenerator(name = "portfolio_snapshots_seq", sequenceName = "PORTFOLIO_SNAPSHOTS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "SNAPSHOT_DATE", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "TOTAL_ASSET", nullable = false)
    private BigDecimal totalAsset;

    @Column(name = "CASH", nullable = false)
    private BigDecimal cash;

    @Column(name = "STOCK_VALUE", nullable = false)
    private BigDecimal stockValue;

    @Builder
    private PortfolioSnapshot(Long accountId, LocalDate snapshotDate, BigDecimal cash, BigDecimal stockValue) {
        this.accountId = accountId;
        this.snapshotDate = snapshotDate;
        this.cash = cash;
        this.stockValue = stockValue;
        this.totalAsset = cash.add(stockValue);
    }
}