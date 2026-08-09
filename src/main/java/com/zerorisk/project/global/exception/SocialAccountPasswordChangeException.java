package com.zerorisk.project.global.exception;

public class SocialAccountPasswordChangeException extends RuntimeException {
    public SocialAccountPasswordChangeException() {
        super("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
    }
}
