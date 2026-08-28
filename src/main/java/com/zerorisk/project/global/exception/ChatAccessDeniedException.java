package com.zerorisk.project.global.exception;

public class ChatAccessDeniedException extends RuntimeException {
    public ChatAccessDeniedException(String message) {
        super(message);
    }
}
