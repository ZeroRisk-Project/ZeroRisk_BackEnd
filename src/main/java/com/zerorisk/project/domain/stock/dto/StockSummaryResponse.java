package com.zerorisk.project.domain.stock.dto;

import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;

public record StockSummaryResponse(String code, String name, Market market) {

    public static StockSummaryResponse from(Stock stock) {
        return new StockSummaryResponse(stock.getCode(), stock.getName(), stock.getMarket());
    }
}