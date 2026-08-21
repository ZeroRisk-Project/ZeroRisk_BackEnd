package com.zerorisk.project.domain.systemnotice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SystemNoticeErrorCode {

    NOT_FOUND("SYSTEM_NOTICE_001", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
