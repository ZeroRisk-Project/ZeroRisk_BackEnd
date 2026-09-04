package com.zerorisk.project.domain.chat.scheduler;

import com.zerorisk.project.domain.chat.service.ChatRetentionService;
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
public class ChatRetentionScheduler {

    private final ChatRetentionService chatRetentionService;

    // 매일 새벽 4시 실행 (트래픽 적은 시간대)
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanup() {
        log.info("채팅 메시지 보관 정책 정리 배치 시작");

        chatRetentionService.cleanupStockMessages();
        chatRetentionService.cleanupCompetitionMessages();

        log.info("채팅 메시지 보관 정책 정리 배치 완료");
    }
}
