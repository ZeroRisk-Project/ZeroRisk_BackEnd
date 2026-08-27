package com.zerorisk.project.global.exception;

import com.zerorisk.project.domain.order.dto.OrderSummaryResponse;
import java.util.List;

public record PendingOrdersExistResponse(
        boolean success,
        String errorCode,
        String message,
        List<OrderSummaryResponse> pendingOrders) {

    public PendingOrdersExistResponse(String errorCode, String message, List<OrderSummaryResponse> pendingOrders) {
        this(false, errorCode, message, pendingOrders);
    }
}
