package com.zerorisk.project.domain.stock.repository;

import com.zerorisk.project.domain.stock.entity.StockDailyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockDailyPriceRepository extends JpaRepository<StockDailyPrice, Long> {
    boolean existsByStockIdAndPriceDate(Long stockId, LocalDate priceDate);

    boolean existsByStockId(Long stockId);

    List<StockDailyPrice> findByStockIdAndPriceDateBetweenOrderByPriceDateAsc(
            Long stockId, LocalDate from, LocalDate to);
}
