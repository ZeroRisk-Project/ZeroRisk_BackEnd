package com.zerorisk.project.domain.stock.dto;

import java.math.BigDecimal;

public record StockRankingResponse(
        String code,
        String name,
        Long currentPrice,
        Long changeAmount,
        BigDecimal changeRate,
        Long volume) {
}