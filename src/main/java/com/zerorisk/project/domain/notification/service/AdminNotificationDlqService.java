package com.zerorisk.project.domain.notification.service;

import com.zerorisk.project.domain.notification.dto.NotificationDlqResponse;
import com.zerorisk.project.domain.notification.dto.NotificationResponse;
import com.zerorisk.project.domain.notification.entity.NotificationDlq;
import com.zerorisk.project.domain.notification.entity.NotificationDlqStatus;
import com.zerorisk.project.domain.notification.repository.NotificationDlqRepository;
import com.zerorisk.project.global.exception.NotificationDlqNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNotificationDlqService {

    private final NotificationDlqRepository notificationDlqRepository;
    private final NotificationSseSender notificationSseSender;
    private final NotificationMetrics notificationMetrics;

    public Page<NotificationDlqResponse> getPendingItems(Pageable pageable) {
        return notificationDlqRepository.findByStatus(NotificationDlqStatus.PENDING, pageable)
                .map(NotificationDlqResponse::from);
    }

    // 재발송 시도. 이번에도 실패하면 NotificationSseSender의 재시도+Recover가 그대로 다시 동작해서
    // 새 DLQ 항목이 하나 더 쌓임(기존 항목은 RESOLVED로 남고, 새 실패가 새 레코드로 기록되는 구조).
    @Transactional
    public void retry(Long dlqId) {
        NotificationDlq dlq = notificationDlqRepository.findByIdAndStatus(dlqId, NotificationDlqStatus.PENDING)
                .orElseThrow(NotificationDlqNotFoundException::new);

        NotificationResponse response = NotificationResponse.from(dlq);

        boolean delivered = notificationSseSender.send(dlq.getUser().getId(), response);

        if (delivered) {
            dlq.markResolved();
            notificationMetrics.recordDlqResolved();
        }
        // delivered가 false면 recover()가 이미 새 DLQ 항목을 저장했고, 이 dlq는 PENDING 상태로 그대로 남음
    }

    @Transactional
    public void ignore(Long dlqId) {
        NotificationDlq dlq = notificationDlqRepository.findByIdAndStatus(dlqId, NotificationDlqStatus.PENDING)
                .orElseThrow(NotificationDlqNotFoundException::new);

        dlq.markIgnored();
        notificationMetrics.recordDlqIgnored();
    }
}
