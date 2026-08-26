package com.zerorisk.project.domain.pricealert.dto;

import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.entity.PriceAlertDirection;
import com.zerorisk.project.domain.stock.entity.Stock;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceAlertResponse(
        Long alertId,
        String stockCode,
        String stockName,
        BigDecimal targetPrice,
        PriceAlertDirection direction,
        LocalDateTime createdAt) {

    public static PriceAlertResponse of(PriceAlert alert, Stock stock) {
        return new PriceAlertResponse(
                alert.getId(),
                stock.getCode(),
                stock.getName(),
                alert.getTargetPrice(),
                alert.getDirection(),
                alert.getCreatedAt());
    }
}