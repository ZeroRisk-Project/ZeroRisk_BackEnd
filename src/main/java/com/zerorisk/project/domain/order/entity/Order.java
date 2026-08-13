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
@Table(name = "ORDERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "ORDERS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SIDE", nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE", nullable = false, length = 10)
    private OrderType orderType;

    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;

    @Column(name = "LIMIT_PRICE")
    private BigDecimal limitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    private OrderStatus status;

    @Column(name = "FILLED_PRICE")
    private BigDecimal filledPrice;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "FILLED_AT")
    private LocalDateTime filledAt;

    @Builder
    private Order(Long accountId, Long stockId, OrderSide side, OrderType orderType, Long quantity, BigDecimal limitPrice) {
        this.accountId = accountId;
        this.stockId = stockId;
        this.side = side;
        this.orderType = orderType;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void fill(BigDecimal price) {
        this.status = OrderStatus.FILLED;
        this.filledPrice = price;
        this.filledAt = LocalDateTime.now();
    }
}