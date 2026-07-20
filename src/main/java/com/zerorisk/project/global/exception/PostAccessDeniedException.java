package com.zerorisk.project.global.exception;

public class PostAccessDeniedException extends RuntimeException {
    public PostAccessDeniedException() {
        super("본인이 작성한 게시글만 수정/삭제할 수 있습니다.");
    }
}