package com.zerorisk.project.domain.pricealert.scheduler;

import com.zerorisk.project.domain.pricealert.service.PriceAlertService;
import com.zerorisk.project.global.security.ratelimit.CooldownChecker;
import java.time.Duration;
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
public class PriceAlertScheduler {

    // cron이 5분마다 도는데, 한 회차가 이보다 오래 걸리면(알림/종목 수가 많아지면) 다음 회차와
    // 겹쳐서 같은 알림이 중복 발송될 수 있다. Redis 락으로 겹침을 막고, 혹시 프로세스가 죽어도
    // 다음 회차 전에는 항상 풀리도록 5분보다 짧은 TTL을 둔다.
    private static final String DISPATCH_LOCK_KEY = "price_alert_dispatch:lock";
    private static final Duration DISPATCH_LOCK_TTL = Duration.ofMinutes(4).plusSeconds(30);

    private final PriceAlertService priceAlertService;
    private final CooldownChecker cooldownChecker;

    @Scheduled(cron = "0 */5 9-15 * * MON-FRI", zone = "Asia/Seoul")
    public void dispatchAlerts() {
        if (!cooldownChecker.tryAcquire(DISPATCH_LOCK_KEY, DISPATCH_LOCK_TTL)) {
            log.warn("목표가 알림 발송 배치가 이미 실행 중이라 이번 회차는 건너뜁니다.");
            return;
        }

        log.info("목표가 알림 발송 배치를 시작합니다.");
        try {
            priceAlertService.dispatchAlerts();
        } catch (Exception e) {
            log.error("목표가 알림 발송 배치 실행 중 오류가 발생했습니다.", e);
        } finally {
            cooldownChecker.release(DISPATCH_LOCK_KEY);
        }
    }
}