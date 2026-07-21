package com.zerorisk.project.domain.openbanking.exception;

import lombok.Getter;

@Getter
public class OpenBankingException extends RuntimeException {

    private final OpenBankingErrorCode errorCode;

    public OpenBankingException(OpenBankingErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}