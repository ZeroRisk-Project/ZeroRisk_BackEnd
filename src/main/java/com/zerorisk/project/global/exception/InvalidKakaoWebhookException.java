package com.zerorisk.project.global.exception;

public class InvalidKakaoWebhookException extends RuntimeException {
    public InvalidKakaoWebhookException() {
        super("유효하지 않은 카카오 웹훅 요청입니다.");
    }
}
