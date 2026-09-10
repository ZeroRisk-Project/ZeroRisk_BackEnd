package com.zerorisk.project.domain.stock.dto;

import java.util.List;

// sellLevels는 현재가에서 먼 호가(10단계)부터 가까운 호가(1단계) 순, buyLevels는 현재가에 가까운
// 호가(1단계)부터 먼 호가(10단계) 순 - 화면에 위에서 아래로 그대로 나열하면 매도호가는 위쪽에,
// 현재가와 가장 가까운 호가일수록 중앙(현재가 표시줄)에 붙어 보이는 자연스러운 호가창 순서가 된다.
public record OrderBookResponse(
        List<OrderBookLevel> sellLevels,
        List<OrderBookLevel> buyLevels,
        long totalSellQuantity,
        long totalBuyQuantity) {
}
