package com.zerorisk.project.domain.order.dto;

import com.zerorisk.project.domain.order.entity.Order;
import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.OrderStatus;
import com.zerorisk.project.domain.order.entity.OrderType;
import com.zerorisk.project.domain.stock.entity.Stock;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        String stockCode,
        String stockName,
        OrderSide side,
        OrderType orderType,
        Long quantity,
        BigDecimal limitPrice,
        BigDecimal filledPrice,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime filledAt) {

    public static OrderSummaryResponse of(Order order, Stock stock) {
        return new OrderSummaryResponse(
                order.getId(),
                stock.getCode(),
                stock.getName(),
                order.getSide(),
                order.getOrderType(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getFilledPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getFilledAt());
    }
}