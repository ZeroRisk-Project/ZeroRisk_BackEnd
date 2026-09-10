package com.zerorisk.project.domain.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioRiskResponse(boolean available, BigDecimal beta, BigDecimal volatility) {

    public static PortfolioRiskResponse unavailable() {
        return new PortfolioRiskResponse(false, null, null);
    }
}
