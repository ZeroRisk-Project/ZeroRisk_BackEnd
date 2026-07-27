// 기존 예외 클래스들과 같은 패턴으로
package com.zerorisk.project.global.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("인증번호가 일치하지 않거나 만료되었습니다.");
    }
}