package com.zerorisk.project.domain.notification.service;

import com.zerorisk.project.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetentionService {

    private static final int RETENTION_DAYS = 60;

    private final NotificationRepository notificationRepository;

    // 60일 지난 알림 물리 삭제 (읽음 여부 무관)
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoff);

        log.info("알림 보관 정책 정리 완료 - 삭제된 알림: {}건", deletedCount);
    }
}
