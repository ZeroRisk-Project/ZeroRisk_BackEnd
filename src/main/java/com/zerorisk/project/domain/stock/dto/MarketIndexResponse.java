package com.zerorisk.project.domain.stock.dto;

import com.zerorisk.project.domain.stock.entity.Market;
import java.math.BigDecimal;

public record MarketIndexResponse(
        Market market,
        BigDecimal value,
        BigDecimal changeAmount,
        BigDecimal changeRate) {
}
