package com.zerorisk.project.domain.order.dto;

import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.Trade;
import com.zerorisk.project.domain.stock.entity.Stock;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
        Long tradeId,
        String stockCode,
        String stockName,
        OrderSide side,
        Long quantity,
        BigDecimal price,
        LocalDateTime tradedAt) {

    public static TradeResponse of(Trade trade, Stock stock) {
        return new TradeResponse(
                trade.getId(),
                stock.getCode(),
                stock.getName(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradedAt());
    }
}