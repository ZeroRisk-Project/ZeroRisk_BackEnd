package com.zerorisk.project.domain.profile.controller;

import com.zerorisk.project.domain.profile.dto.ProfileResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsUpdateRequest;
import com.zerorisk.project.domain.profile.service.ProfileService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ProfileResponse getProfile(
            @PathVariable Long userId,
            @CurrentUserId Long viewerId) {
        return profileService.getProfile(userId, viewerId);
    }

    @GetMapping("/me/settings")
    public ProfileSettingsResponse getMySettings(@CurrentUserId Long userId) {
        return profileService.getMySettings(userId);
    }

    @PutMapping("/me/settings")
    public ProfileSettingsResponse updateMySettings(
            @CurrentUserId Long userId,
            @Valid @RequestBody ProfileSettingsUpdateRequest request) {
        return profileService.updateMySettings(userId, request);
    }
}
