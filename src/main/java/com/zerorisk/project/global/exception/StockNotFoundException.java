package com.zerorisk.project.global.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException() {
        super("종목을 찾을 수 없습니다.");
    }
}