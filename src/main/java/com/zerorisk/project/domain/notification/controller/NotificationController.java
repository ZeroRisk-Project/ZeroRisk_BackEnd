package com.zerorisk.project.domain.notification.controller;

import com.zerorisk.project.domain.notification.dto.NotificationResponse;
import com.zerorisk.project.domain.notification.service.NotificationService;
import com.zerorisk.project.domain.notification.service.SseEmitterService;
import com.zerorisk.project.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@CurrentUserId Long userId) {
        return sseEmitterService.subscribe(userId);
    }

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(@CurrentUserId Long userId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @PatchMapping("/api/v1/notifications/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@CurrentUserId Long userId, @PathVariable Long notificationId) {
        notificationService.markAsRead(userId, notificationId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(@CurrentUserId Long userId) {
        notificationService.markAllAsRead(userId);

        return ResponseEntity.noContent().build();
    }
}