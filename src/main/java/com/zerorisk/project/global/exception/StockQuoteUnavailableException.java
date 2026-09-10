package com.zerorisk.project.global.exception;

public class StockQuoteUnavailableException extends RuntimeException {
    public StockQuoteUnavailableException(Throwable cause) {
        this("현재가 조회에 실패했습니다. 잠시 후 다시 시도해주세요.", cause);
    }

    public StockQuoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
