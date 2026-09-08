package com.zerorisk.project.global.exception;

public class ChatMessageEmptyException extends RuntimeException {
    public ChatMessageEmptyException() {
        super("메시지 내용 또는 이미지 중 하나는 반드시 있어야 합니다.");
    }
}
