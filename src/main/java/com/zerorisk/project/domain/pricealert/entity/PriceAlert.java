package com.zerorisk.project.domain.pricealert.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRICE_ALERTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "price_alerts_seq")
    @SequenceGenerator(name = "price_alerts_seq", sequenceName = "PRICE_ALERTS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Column(name = "TARGET_PRICE", nullable = false)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTION", nullable = false, length = 10)
    private PriceAlertDirection direction;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PriceAlert(Long userId, Long stockId, BigDecimal targetPrice, PriceAlertDirection direction) {
        this.userId = userId;
        this.stockId = stockId;
        this.targetPrice = targetPrice;
        this.direction = direction;
        this.createdAt = LocalDateTime.now();
    }
}