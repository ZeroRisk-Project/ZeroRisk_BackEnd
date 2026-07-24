package com.zerorisk.project.domain.openbanking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MonthlyChargeSettingRequest(
        @NotNull @Min(1) @Max(28) Integer chargeDay,

        @NotNull @DecimalMin(value = "1", message = "충전 금액은 1원 이상이어야 합니다.") BigDecimal chargeAmount) {
}