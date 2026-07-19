package com.zerorisk.project.domain.user.controller;

import com.zerorisk.project.domain.user.dto.MyProfileResponse;
import com.zerorisk.project.domain.user.dto.UpdateProfileRequest;
import com.zerorisk.project.domain.user.service.UserService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public MyProfileResponse getMyProfile(@CurrentUserId Long userId) {
        return userService.getMyProfile(userId);
    }

    @PatchMapping
    public MyProfileResponse updateMyProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(userId, request);
    }
}