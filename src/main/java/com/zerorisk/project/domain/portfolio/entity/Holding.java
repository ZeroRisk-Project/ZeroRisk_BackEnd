package com.zerorisk.project.domain.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "HOLDINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "holdings_seq")
    @SequenceGenerator(name = "holdings_seq", sequenceName = "HOLDINGS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @Column(name = "AVERAGE_PRICE", nullable = false)
    private BigDecimal averagePrice;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Holding(Long accountId, Long stockId, Long quantity, BigDecimal averagePrice) {
        this.accountId = accountId;
        this.stockId = stockId;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyBuy(Long boughtQuantity, BigDecimal boughtPrice) {
        BigDecimal existingCost = averagePrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal boughtCost = boughtPrice.multiply(BigDecimal.valueOf(boughtQuantity));
        long newQuantity = quantity + boughtQuantity;

        this.averagePrice = existingCost.add(boughtCost)
                .divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void applySell(Long soldQuantity) {
        this.quantity = this.quantity - soldQuantity;
        this.updatedAt = LocalDateTime.now();
    }
}