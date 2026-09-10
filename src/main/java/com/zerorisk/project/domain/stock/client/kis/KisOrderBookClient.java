package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisOrderBookResponse;

public interface KisOrderBookClient {
    KisOrderBookResponse.Output1 fetchOrderBook(String code);
}
