package com.zerorisk.project.domain.notification.controller;

import com.zerorisk.project.domain.notification.dto.NotificationDlqResponse;
import com.zerorisk.project.domain.notification.service.AdminNotificationDlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 권한 체크는 SecurityConfig의 /api/v1/admin/** 일괄 규칙으로 처리됨
@RestController
@RequestMapping("/api/v1/admin/notifications/dlq")
@RequiredArgsConstructor
public class AdminNotificationDlqController {

    private final AdminNotificationDlqService adminNotificationDlqService;

    @GetMapping
    public Page<NotificationDlqResponse> getPendingItems(Pageable pageable) {
        return adminNotificationDlqService.getPendingItems(pageable);
    }

    @PatchMapping("/{dlqId}/retry")
    public void retry(@PathVariable Long dlqId) {
        adminNotificationDlqService.retry(dlqId);
    }

    @PatchMapping("/{dlqId}/ignore")
    public void ignore(@PathVariable Long dlqId) {
        adminNotificationDlqService.ignore(dlqId);
    }
}
