package com.zerorisk.project.domain.stock.scheduler;

import com.zerorisk.project.domain.stock.service.StockDailyPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// app.scheduling.enabled=false(벤치마크 프로필 등)일 때만 비활성화 - PortfolioSnapshotScheduler와 동일 패턴.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StockDailyPriceScheduler {

    private final StockDailyPriceService stockDailyPriceService;

    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void createDailyPrices() {
        log.info("종목 일별 종가 저장 배치를 시작합니다.");
        try {
            stockDailyPriceService.createDailyPrices();
        } catch (Exception e) {
            log.error("종목 일별 종가 저장 배치 실행 중 오류가 발생했습니다.", e);
        }
    }
}
