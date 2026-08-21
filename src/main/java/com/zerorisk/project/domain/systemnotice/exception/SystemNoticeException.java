package com.zerorisk.project.domain.systemnotice.exception;

import lombok.Getter;

@Getter
public class SystemNoticeException extends RuntimeException {

    private final SystemNoticeErrorCode errorCode;

    public SystemNoticeException(SystemNoticeErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
