package com.zerorisk.project.domain.notification.service;

import com.zerorisk.project.domain.notification.dto.NotificationResponse;
import com.zerorisk.project.domain.notification.entity.AlertSettings;
import com.zerorisk.project.domain.notification.entity.Notification;
import com.zerorisk.project.domain.notification.entity.NotificationType;
import com.zerorisk.project.domain.notification.repository.NotificationRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.NotificationNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AlertSettingsService alertSettingsService;
    private final SseEmitterService sseEmitterService;

    // 다른 도메인(주문/댓글/대회/알림/문의)에서 이 메서드를 호출해서 알림을 생성하게 됨
    @Transactional
    public void createNotification(
            Long userId, NotificationType type, String title, String message, String targetUrl) {
        AlertSettings settings = alertSettingsService.getOrCreateSettings(userId);

        if (!settings.isEnabled(type)) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .targetUrl(targetUrl)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        sseEmitterService.send(userId, NotificationResponse.from(savedNotification));
    }

    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);

        unreadNotifications.forEach(Notification::markAsRead);
    }
}