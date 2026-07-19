package com.zerorisk.project.global.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("유효하지 않거나 만료된 토큰입니다. 다시 로그인해주세요.");
    }
}