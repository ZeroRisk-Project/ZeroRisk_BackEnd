package com.zerorisk.project.domain.stock.dto;

import com.zerorisk.project.domain.stock.entity.Market;
import java.math.BigDecimal;

public record StockDetailResponse(
        String code,
        String name,
        Market market,
        Long currentPrice,
        Long changeAmount,
        BigDecimal changeRate,
        Long week52High,
        Long week52Low) {
}