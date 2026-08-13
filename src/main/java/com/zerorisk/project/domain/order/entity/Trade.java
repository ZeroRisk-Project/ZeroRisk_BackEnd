package com.zerorisk.project.domain.order.entity;

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
@Table(name = "TRADES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trades_seq")
    @SequenceGenerator(name = "trades_seq", sequenceName = "TRADES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SIDE", nullable = false, length = 10)
    private OrderSide side;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @Column(name = "PRICE", nullable = false)
    private BigDecimal price;

    @Column(name = "TRADED_AT", nullable = false, updatable = false)
    private LocalDateTime tradedAt;

    @Builder
    private Trade(Long orderId, Long accountId, Long stockId, OrderSide side, Long quantity, BigDecimal price) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.stockId = stockId;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.tradedAt = LocalDateTime.now();
    }
}