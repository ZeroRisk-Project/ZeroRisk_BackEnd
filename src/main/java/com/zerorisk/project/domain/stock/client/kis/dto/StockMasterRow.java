package com.zerorisk.project.domain.stock.client.kis.dto;

import com.zerorisk.project.domain.stock.entity.Market;

public record StockMasterRow(String code, String standardCode, String name, Market market) {

}