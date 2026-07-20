package com.zerorisk.project.domain.user.controller;

import com.zerorisk.project.domain.user.dto.AdminUserResponse;
import com.zerorisk.project.domain.user.dto.UserSuspendRequest;
import com.zerorisk.project.domain.user.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: SecurityConfig에서 /api/v1/admin/** 경로에 ADMIN 권한 체크 적용 필요 (박종권님 설정 확인)
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return adminUserService.getUsers(pageable);
    }

    @PatchMapping("/{userId}/suspend")
    public AdminUserResponse suspendUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserSuspendRequest request) {
        return adminUserService.suspendUser(userId, request);
    }

    @PatchMapping("/{userId}/unsuspend")
    public AdminUserResponse unsuspendUser(@PathVariable Long userId) {
        return adminUserService.unsuspendUser(userId);
    }
}