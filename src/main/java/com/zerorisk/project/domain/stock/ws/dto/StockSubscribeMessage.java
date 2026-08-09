package com.zerorisk.project.domain.stock.ws.dto;

public record StockSubscribeMessage(SubscribeAction action, String code) {

    public enum SubscribeAction {
        SUBSCRIBE,
        UNSUBSCRIBE
    }
}