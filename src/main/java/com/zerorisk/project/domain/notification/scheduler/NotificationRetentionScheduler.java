package com.zerorisk.project.domain.notification.scheduler;

import com.zerorisk.project.domain.notification.service.NotificationRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetentionScheduler {

    private final NotificationRetentionService notificationRetentionService;

    // 매일 새벽 5시 실행 (채팅 정리 배치와 시간대 분산)
    @Scheduled(cron = "0 0 5 * * *")
    public void cleanup() {
        log.info("알림 보관 정책 정리 배치 시작");

        notificationRetentionService.cleanup();

        log.info("알림 보관 정책 정리 배치 완료");
    }
}
