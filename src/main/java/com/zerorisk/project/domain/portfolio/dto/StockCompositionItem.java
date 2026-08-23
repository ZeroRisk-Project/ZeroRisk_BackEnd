package com.zerorisk.project.domain.portfolio.dto;

import java.math.BigDecimal;

public record StockCompositionItem(
        String stockCode,
        String stockName,
        BigDecimal evaluationAmount,
        BigDecimal weight) {
}