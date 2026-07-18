package com.zerorisk.project.global.exception;

public record ErrorResponse(
        boolean success,
        String errorCode,
        String message) {
    public ErrorResponse(String errorCode, String message) {
        this(false, errorCode, message);
    }
}