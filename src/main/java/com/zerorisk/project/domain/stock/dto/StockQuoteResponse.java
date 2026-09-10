package com.zerorisk.project.domain.stock.dto;

import java.math.BigDecimal;

public record StockQuoteResponse(
        String code,
        Long currentPrice,
        Long changeAmount,
        BigDecimal changeRate) {
}
