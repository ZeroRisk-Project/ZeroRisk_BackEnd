package com.zerorisk.project.global.exception;

public class CaptchaRequiredException extends RuntimeException {
    public CaptchaRequiredException() {
        super("CAPTCHA 인증이 필요합니다.");
    }
}
