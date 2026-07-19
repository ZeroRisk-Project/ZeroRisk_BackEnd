package com.zerorisk.project.global.exception;

public class ReportTargetNotFoundException extends RuntimeException {
    public ReportTargetNotFoundException() {
        super("신고 대상을 찾을 수 없습니다.");
    }
}