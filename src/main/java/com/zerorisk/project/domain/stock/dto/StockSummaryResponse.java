package com.zerorisk.project.domain.stock.dto;

import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockCodeType;

public record StockSummaryResponse(Long id, String code, String name, Market market, boolean preferred) {

    public static StockSummaryResponse from(Stock stock) {
        return new StockSummaryResponse(
                stock.getId(),
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                StockCodeType.isPreferred(stock.getCode()));
    }
}