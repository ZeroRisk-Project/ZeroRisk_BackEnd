package com.zerorisk.project.global.exception;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException() {
        super("신고 내역을 찾을 수 없습니다.");
    }
}