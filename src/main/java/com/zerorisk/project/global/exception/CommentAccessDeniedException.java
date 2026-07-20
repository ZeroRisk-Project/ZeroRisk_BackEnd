package com.zerorisk.project.global.exception;

public class CommentAccessDeniedException extends RuntimeException {
    public CommentAccessDeniedException() {
        super("본인이 작성한 댓글만 수정/삭제할 수 있습니다.");
    }
}