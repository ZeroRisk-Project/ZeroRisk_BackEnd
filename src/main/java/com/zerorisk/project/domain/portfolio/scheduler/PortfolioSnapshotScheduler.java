package com.zerorisk.project.domain.portfolio.scheduler;

import com.zerorisk.project.domain.portfolio.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotService portfolioSnapshotService;

    @Scheduled(cron = "0 0 16 * * MON-FRI", zone = "Asia/Seoul")
    public void createDailySnapshots() {
        log.info("일별 자산 스냅샷 생성 배치를 시작합니다.");
        try {
            portfolioSnapshotService.createDailySnapshots();
        } catch (Exception e) {
            log.error("일별 자산 스냅샷 생성 배치 실행 중 오류가 발생했습니다.", e);
        }
    }
}