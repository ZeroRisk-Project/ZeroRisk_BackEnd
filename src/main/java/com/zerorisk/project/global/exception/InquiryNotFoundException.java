package com.zerorisk.project.global.exception;

public class InquiryNotFoundException extends RuntimeException {
    public InquiryNotFoundException() {
        super("문의 내역을 찾을 수 없습니다.");
    }
}