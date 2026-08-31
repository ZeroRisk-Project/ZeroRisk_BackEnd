package com.zerorisk.project.global.exception;

public class NotificationDlqNotFoundException extends RuntimeException {
    public NotificationDlqNotFoundException() {
        super("DLQ 알림 항목을 찾을 수 없습니다.");
    }
}
