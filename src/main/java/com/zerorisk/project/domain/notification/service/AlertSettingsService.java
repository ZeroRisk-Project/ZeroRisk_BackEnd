package com.zerorisk.project.domain.notification.service;

import com.zerorisk.project.domain.notification.dto.AlertSettingsResponse;
import com.zerorisk.project.domain.notification.dto.AlertSettingsUpdateRequest;
import com.zerorisk.project.domain.notification.entity.AlertSettings;
import com.zerorisk.project.domain.notification.repository.AlertSettingsRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertSettingsService {

    private final AlertSettingsRepository alertSettingsRepository;
    private final UserRepository userRepository;

    @Transactional
    public AlertSettingsResponse getSettings(Long userId) {
        return AlertSettingsResponse.from(getOrCreateSettings(userId));
    }

    @Transactional
    public AlertSettingsResponse updateSettings(Long userId, AlertSettingsUpdateRequest request) {
        AlertSettings settings = getOrCreateSettings(userId);

        settings.update(
                request.orderFilled(),
                request.commentAdded(),
                request.competition(),
                request.priceAlert(),
                request.inquiryAnswered());

        return AlertSettingsResponse.from(settings);
    }

    AlertSettings getOrCreateSettings(Long userId) {
        return alertSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    private AlertSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        AlertSettings settings = AlertSettings.builder()
                .user(user)
                .build();

        return alertSettingsRepository.save(settings);
    }
}