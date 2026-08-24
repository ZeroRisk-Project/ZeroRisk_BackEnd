package com.zerorisk.project.domain.portfolio.dto;

import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioSnapshotResponse(
        LocalDate snapshotDate,
        BigDecimal cash,
        BigDecimal stockValue,
        BigDecimal totalAsset) {

    public static PortfolioSnapshotResponse from(PortfolioSnapshot snapshot) {
        return new PortfolioSnapshotResponse(
                snapshot.getSnapshotDate(),
                snapshot.getCash(),
                snapshot.getStockValue(),
                snapshot.getTotalAsset());
    }
}