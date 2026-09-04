package com.zerorisk.project.domain.notification.scheduler;

import com.zerorisk.project.domain.notification.service.NotificationRetentionService;
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
