package com.zerorisk.project.domain.stock.entity;

public final class StockCodeType {

    private StockCodeType() {
    }

    // 국내 상장 종목의 6자리 코드는 마지막 자리가 0이면 보통주, 0이 아니면(5=구형우선주,
    // 1/6/7/8/9=신형우선주 등) 우선주를 의미한다.
    public static boolean isPreferred(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return code.charAt(code.length() - 1) != '0';
    }
}
