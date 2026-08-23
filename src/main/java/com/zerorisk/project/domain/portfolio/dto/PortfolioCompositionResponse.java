package com.zerorisk.project.domain.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioCompositionResponse(
        BigDecimal cash,
        BigDecimal stockValue,
        BigDecimal totalAsset,
        BigDecimal cashRatio,
        BigDecimal stockRatio,
        List<StockCompositionItem> stocks) {
}