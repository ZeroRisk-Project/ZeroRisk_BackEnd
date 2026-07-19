package com.zerorisk.project.global.exception;

public class FollowNotFoundException extends RuntimeException {
    public FollowNotFoundException() {
        super("팔로우 관계를 찾을 수 없습니다.");
    }
}