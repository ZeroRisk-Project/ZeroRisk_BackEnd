package com.zerorisk.project.domain.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "MARKET_INDEX_DAILY_PRICES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketIndexDailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "market_index_daily_prices_seq")
    @SequenceGenerator(name = "market_index_daily_prices_seq", sequenceName = "MARKET_INDEX_DAILY_PRICES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "MARKET", nullable = false, length = 10)
    private Market market;

    @Column(name = "PRICE_DATE", nullable = false)
    private LocalDate priceDate;

    @Column(name = "CLOSE_VALUE", nullable = false)
    private BigDecimal closeValue;

    @Builder
    private MarketIndexDailyPrice(Market market, LocalDate priceDate, BigDecimal closeValue) {
        this.market = market;
        this.priceDate = priceDate;
        this.closeValue = closeValue;
    }
}
