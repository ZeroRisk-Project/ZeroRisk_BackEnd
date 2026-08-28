package com.zerorisk.project.global.exception;

public class ChatMessageNotFoundException extends RuntimeException {
    public ChatMessageNotFoundException() {
        super("채팅 메시지를 찾을 수 없습니다.");
    }
}
