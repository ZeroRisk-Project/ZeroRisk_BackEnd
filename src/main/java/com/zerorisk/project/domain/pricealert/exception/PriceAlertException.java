package com.zerorisk.project.domain.pricealert.exception;

import lombok.Getter;

@Getter
public class PriceAlertException extends RuntimeException {

    private final PriceAlertErrorCode errorCode;

    public PriceAlertException(PriceAlertErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}