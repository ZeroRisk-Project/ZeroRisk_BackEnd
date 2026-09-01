package com.zerorisk.project.domain.notification.service;

import com.zerorisk.project.domain.notification.dto.NotificationResponse;
import com.zerorisk.project.domain.notification.entity.NotificationDlq;
import com.zerorisk.project.domain.notification.repository.NotificationDlqRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseSender {

    private final SseEmitterService sseEmitterService;
    private final NotificationDlqRepository notificationDlqRepository;
    private final UserRepository userRepository;
    private final NotificationMetrics notificationMetrics;

    // 1초 -> 2초로 지수 증가, ±10% Jitter로 재시도 시점 분산. 최대 3회 시도(원본 1 + 재시도 2).
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, random = true))
    public boolean send(Long userId, NotificationResponse response) {
        if (RetrySynchronizationManager.getContext() != null
                && RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            notificationMetrics.recordRetry();
        }

        sseEmitterService.send(userId, response);

        return true;
    }

    // 재시도 전부 실패 시 호출됨. 반환 타입은 send()와 동일해야 함(boolean).
    @Recover
    @Transactional
    public boolean recover(Exception e, Long userId, NotificationResponse response) {
        log.warn("SSE 알림 전송 재시도 전부 실패, DLQ로 격리 - userId: {}, notificationId: {}",
                userId, response.id(), e);

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        NotificationDlq dlq = NotificationDlq.builder()
                .user(user)
                .type(response.type())
                .title(response.title())
                .message(response.message())
                .targetUrl(response.targetUrl())
                .failureReason(e.getMessage())
                .retryCount(3)
                .build();

        notificationDlqRepository.save(dlq);
        notificationMetrics.recordDlqSaved();

        return false;
    }
}
