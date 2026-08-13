package com.zerorisk.project.domain.stock.ws.dto;

import java.math.BigDecimal;

public record StockPriceMessage(
        String code,
        Long currentPrice,
        Long changeAmount,
        BigDecimal changeRate) {
}