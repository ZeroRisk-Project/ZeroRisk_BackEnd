package com.zerorisk.project.global.exception;

public class PracticeCreditNotEligibleException extends RuntimeException {
    public PracticeCreditNotEligibleException() {
        super("연습용 크레딧을 받을 수 없는 상태입니다.");
    }
}
