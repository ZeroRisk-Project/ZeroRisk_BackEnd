package com.zerorisk.project.domain.user.controller;

import com.zerorisk.project.domain.user.dto.AdminUserResponse;
import com.zerorisk.project.domain.user.dto.UserSuspendRequest;
import com.zerorisk.project.domain.user.entity.UserStatus;
import com.zerorisk.project.domain.user.service.AdminUserService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return adminUserService.getUsers(keyword, status, pageable);
    }

    @PatchMapping("/{userId}/suspend")
    public AdminUserResponse suspendUser(
            @CurrentUserId Long adminId,
            @PathVariable Long userId,
            @Valid @RequestBody UserSuspendRequest request) {
        return adminUserService.suspendUser(adminId, userId, request);
    }

    @PatchMapping("/{userId}/unsuspend")
    public AdminUserResponse unsuspendUser(
            @CurrentUserId Long adminId,
            @PathVariable Long userId) {
        return adminUserService.unsuspendUser(adminId, userId);
    }
}