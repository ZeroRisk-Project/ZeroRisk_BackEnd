package com.zerorisk.project.domain.account.dto;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        BigDecimal balance
) {
}
