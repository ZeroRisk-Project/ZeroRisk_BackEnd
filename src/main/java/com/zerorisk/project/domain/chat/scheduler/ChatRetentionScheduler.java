package com.zerorisk.project.domain.chat.scheduler;

import com.zerorisk.project.domain.chat.service.ChatRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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
