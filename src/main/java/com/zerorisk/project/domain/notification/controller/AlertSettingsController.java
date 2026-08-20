package com.zerorisk.project.domain.notification.controller;

import com.zerorisk.project.domain.notification.dto.AlertSettingsResponse;
import com.zerorisk.project.domain.notification.dto.AlertSettingsUpdateRequest;
import com.zerorisk.project.domain.notification.service.AlertSettingsService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
public class AlertSettingsController {

    private final AlertSettingsService alertSettingsService;

    @GetMapping
    public AlertSettingsResponse getSettings(@CurrentUserId Long userId) {
        return alertSettingsService.getSettings(userId);
    }

    @PutMapping
    public AlertSettingsResponse updateSettings(@CurrentUserId Long userId, @Valid @RequestBody AlertSettingsUpdateRequest request) {
        return alertSettingsService.updateSettings(userId, request);
    }
}