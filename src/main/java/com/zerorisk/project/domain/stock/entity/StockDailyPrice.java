package com.zerorisk.project.domain.stock.entity;

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
@Table(name = "STOCK_DAILY_PRICES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockDailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_daily_prices_seq")
    @SequenceGenerator(name = "stock_daily_prices_seq", sequenceName = "STOCK_DAILY_PRICES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Column(name = "PRICE_DATE", nullable = false)
    private LocalDate priceDate;

    @Column(name = "CLOSE_PRICE", nullable = false)
    private BigDecimal closePrice;

    @Builder
    private StockDailyPrice(Long stockId, LocalDate priceDate, BigDecimal closePrice) {
        this.stockId = stockId;
        this.priceDate = priceDate;
        this.closePrice = closePrice;
    }
}
