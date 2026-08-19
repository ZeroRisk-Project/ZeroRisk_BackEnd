package com.zerorisk.project.domain.order.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode {

    LIMIT_PRICE_REQUIRED("ORDER_001", "지정가 주문은 가격을 입력해야 합니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE("ORDER_002", "주문 가능 금액이 부족합니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_HOLDING("ORDER_003", "보유 수량이 부족합니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("ORDER_004", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_PROCESSED("ORDER_005", "이미 체결되었거나 취소된 주문입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}