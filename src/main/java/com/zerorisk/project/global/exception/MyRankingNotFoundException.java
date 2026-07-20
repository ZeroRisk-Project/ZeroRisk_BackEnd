package com.zerorisk.project.global.exception;

public class MyRankingNotFoundException extends RuntimeException {
    public MyRankingNotFoundException() {
        super("현재 랭킹 데이터가 없습니다. 수익률 공개 설정 또는 자산 데이터를 확인해주세요.");
    }
}