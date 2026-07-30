package com.zerorisk.project.domain.account.exception;

import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {

    private final AccountErrorCode errorCode;

    public AccountException(AccountErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
