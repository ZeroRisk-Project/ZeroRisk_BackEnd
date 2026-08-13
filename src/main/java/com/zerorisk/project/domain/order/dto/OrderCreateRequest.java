package com.zerorisk.project.domain.order.dto;

import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderCreateRequest(
        @NotNull Long accountId,

        @NotNull String stockCode,

        @NotNull OrderSide side,

        @NotNull OrderType orderType,

        @NotNull @Positive Long quantity,

        BigDecimal limitPrice) {
}