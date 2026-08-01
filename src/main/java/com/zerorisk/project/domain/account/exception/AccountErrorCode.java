package com.zerorisk.project.domain.account.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode {

    NOT_FOUND("ACCOUNT_001", "계좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED("ACCOUNT_002", "본인 소유의 계좌만 조회할 수 있습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
