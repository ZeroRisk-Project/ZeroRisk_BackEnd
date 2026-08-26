package com.zerorisk.project.domain.pricealert.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PriceAlertErrorCode {

    NOT_FOUND("PRICE_ALERT_001", "목표가 알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED("PRICE_ALERT_002", "해당 목표가 알림에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}