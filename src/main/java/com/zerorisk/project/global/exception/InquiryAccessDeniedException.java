package com.zerorisk.project.global.exception;

public class InquiryAccessDeniedException extends RuntimeException {
    public InquiryAccessDeniedException() {
        super("본인이 등록한 문의만 조회할 수 있습니다.");
    }
}