package com.zerorisk.project.domain.order.scheduler;

import com.zerorisk.project.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void fillPendingOrders() {
        log.info("예약 주문 체결 배치를 시작합니다.");
        try {
            orderService.fillPendingOrders();
        } catch (Exception e) {
            log.error("예약 주문 체결 배치 실행 중 오류가 발생했습니다.", e);
        }
    }
}