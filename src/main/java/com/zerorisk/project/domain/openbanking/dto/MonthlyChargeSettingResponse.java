package com.zerorisk.project.domain.openbanking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MonthlyChargeSettingResponse(
        Integer chargeDay,
        BigDecimal chargeAmount,
        Boolean isActive,
        LocalDateTime lastChargedAt) {
}