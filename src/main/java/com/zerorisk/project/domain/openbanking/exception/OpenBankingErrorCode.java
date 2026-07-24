package com.zerorisk.project.domain.openbanking.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OpenBankingErrorCode {

    ALREADY_AUTHENTICATED("OPENBANKING_001", "이미 계좌 인증이 완료되었습니다.", HttpStatus.CONFLICT),
    AUTH_NOT_FOUND("OPENBANKING_002", "계좌 인증 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE("OPENBANKING_003", "실계좌 잔액이 부족하여 충전할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CHARGE_LIMIT_EXCEEDED("OPENBANKING_004", "월 충전 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    MONTHLY_CHARGE_SETTING_NOT_FOUND("OPENBANKING_005", "월 충전 설정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MONTHLY_CHARGE_ALREADY_EXISTS("OPENBANKING_006", "이미 월 충전 설정이 존재합니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}