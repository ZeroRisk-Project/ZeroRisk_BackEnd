package com.zerorisk.project.domain.order.exception;

import lombok.Getter;

@Getter
public class OrderException extends RuntimeException {

    private final OrderErrorCode errorCode;

    public OrderException(OrderErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}