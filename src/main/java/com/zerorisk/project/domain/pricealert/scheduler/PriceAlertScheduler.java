package com.zerorisk.project.domain.pricealert.scheduler;

import com.zerorisk.project.domain.pricealert.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertScheduler {

    private final PriceAlertService priceAlertService;

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void dispatchAlerts() {
        log.info("목표가 알림 발송 배치를 시작합니다.");
        try {
            priceAlertService.dispatchAlerts();
        } catch (Exception e) {
            log.error("목표가 알림 발송 배치 실행 중 오류가 발생했습니다.", e);
        }
    }
}