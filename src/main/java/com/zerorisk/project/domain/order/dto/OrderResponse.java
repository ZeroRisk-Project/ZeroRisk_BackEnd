package com.zerorisk.project.domain.order.dto;

import com.zerorisk.project.domain.order.entity.Order;
import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.OrderStatus;
import com.zerorisk.project.domain.order.entity.OrderType;
import java.math.BigDecimal;

public record OrderResponse(
        Long orderId,
        OrderSide side,
        OrderType orderType,
        Long quantity,
        BigDecimal limitPrice,
        OrderStatus status,
        BigDecimal filledPrice) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSide(),
                order.getOrderType(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getStatus(),
                order.getFilledPrice());
    }
}