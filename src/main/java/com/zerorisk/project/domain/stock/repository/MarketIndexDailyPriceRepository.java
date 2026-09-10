package com.zerorisk.project.domain.stock.repository;

import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.MarketIndexDailyPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketIndexDailyPriceRepository extends JpaRepository<MarketIndexDailyPrice, Long> {
    boolean existsByMarketAndPriceDate(Market market, LocalDate priceDate);

    List<MarketIndexDailyPrice> findByMarketAndPriceDateBetweenOrderByPriceDateAsc(
            Market market, LocalDate from, LocalDate to);
}
