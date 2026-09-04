package com.zerorisk.project.domain.stock.scheduler;

import com.zerorisk.project.domain.stock.service.StockMasterSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// app.scheduling.enabled=false(벤치마크 프로필 등)일 때만 비활성화 - 값이 없으면(matchIfMissing)
// 기존과 동일하게 항상 켜짐. 실서비스 등 다른 프로필엔 영향 없음.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StockMasterSyncScheduler {

    private final StockMasterSyncService stockMasterSyncService;

    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void syncStockMaster() {
        log.info("종목 마스터 동기화 배치를 시작합니다.");
        try {
            stockMasterSyncService.sync();
        } catch (Exception e) {
            log.error("종목 마스터 동기화 배치 실행 중 오류가 발생했습니다.", e);
        }
    }
}