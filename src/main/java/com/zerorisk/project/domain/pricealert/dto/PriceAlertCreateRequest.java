package com.zerorisk.project.domain.pricealert.dto;

import com.zerorisk.project.domain.pricealert.entity.PriceAlertDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PriceAlertCreateRequest(
        @NotBlank(message = "종목 코드를 입력해야 합니다.")
        String stockCode,

        @NotNull(message = "목표가를 입력해야 합니다.")
        @Positive(message = "목표가는 0보다 커야 합니다.")
        BigDecimal targetPrice,

        @NotNull(message = "알림 조건을 지정해야 합니다.")
        PriceAlertDirection direction) {
}