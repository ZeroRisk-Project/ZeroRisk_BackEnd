package com.zerorisk.project.domain.user.controller;

import com.zerorisk.project.domain.auth.service.AuthService;
import com.zerorisk.project.domain.user.dto.MyProfileResponse;
import com.zerorisk.project.domain.user.dto.UpdateProfileRequest;
import com.zerorisk.project.domain.user.dto.WithdrawRequest;
import com.zerorisk.project.domain.user.service.UserService;
import com.zerorisk.project.global.security.CookieUtil;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final AuthService authService;
    private final CookieUtil cookieUtil;

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

    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @CurrentUserId Long userId,
            @RequestBody(required = false) WithdrawRequest request,
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        WithdrawRequest safeRequest = request != null ? request : new WithdrawRequest(null);
        userService.withdraw(userId, safeRequest);
        authService.logout(refreshToken);

        ResponseCookie deletedAccess = cookieUtil.delete("accessToken");
        ResponseCookie deletedRefresh = cookieUtil.delete("refreshToken");

        return ResponseEntity.ok()
                .header("Set-Cookie", deletedAccess.toString())
                .header("Set-Cookie", deletedRefresh.toString())
                .build();
    }
}