package com.zerorisk.project.global.exception;

public class DuplicateFollowException extends RuntimeException {
    public DuplicateFollowException() {
        super("이미 팔로우한 사용자입니다.");
    }
}