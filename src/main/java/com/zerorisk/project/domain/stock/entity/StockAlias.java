package com.zerorisk.project.domain.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STOCK_ALIASES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_alias_seq")
    @SequenceGenerator(name = "stock_alias_seq", sequenceName = "STOCK_ALIASES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID", nullable = false)
    private Stock stock;

    @Column(name = "ALIAS", nullable = false, length = 50)
    private String alias;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private StockAlias(Stock stock, String alias) {
        this.stock = stock;
        this.alias = alias;
        this.createdAt = LocalDateTime.now();
    }
}