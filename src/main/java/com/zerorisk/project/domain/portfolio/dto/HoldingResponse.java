package com.zerorisk.project.domain.portfolio.dto;

import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.stock.entity.Stock;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record HoldingResponse(
        Long holdingId,
        String stockCode,
        String stockName,
        Long quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate) {

    public static HoldingResponse of(Holding holding, Stock stock, BigDecimal currentPrice) {
        BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
        BigDecimal purchaseAmount = holding.getAveragePrice().multiply(quantity);
        BigDecimal evaluationAmount = currentPrice.multiply(quantity);
        BigDecimal profitLoss = evaluationAmount.subtract(purchaseAmount);
        BigDecimal profitRate = purchaseAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitLoss.divide(purchaseAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        return new HoldingResponse(
                holding.getId(),
                stock.getCode(),
                stock.getName(),
                holding.getQuantity(),
                holding.getAveragePrice(),
                currentPrice,
                evaluationAmount,
                profitLoss,
                profitRate);
    }
}