package com.zerorisk.project.domain.stock.client.kis;

public interface KisRealtimeClient {

    void subscribe(String code);

    void unsubscribe(String code);
}