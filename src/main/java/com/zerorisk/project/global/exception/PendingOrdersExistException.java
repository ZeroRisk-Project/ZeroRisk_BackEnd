package com.zerorisk.project.global.exception;

import com.zerorisk.project.domain.order.dto.OrderSummaryResponse;
import java.util.List;
import lombok.Getter;

@Getter
public class PendingOrdersExistException extends RuntimeException {

    private final List<OrderSummaryResponse> pendingOrders;

    public PendingOrdersExistException(List<OrderSummaryResponse> pendingOrders) {
        super("체결 대기 중인 주문이 있어 탈퇴할 수 없습니다.");
        this.pendingOrders = pendingOrders;
    }
}
