package com.zerorisk.project.domain.user.dto;

import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        String userRole,
        UserStatus status,
        LocalDateTime suspendedUntil,
        String suspensionReason,
        LocalDateTime createdAt,
        String accountNumMasked) {

    public static AdminUserResponse from(User user, String accountNumMasked) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getUserRole().name(),
                user.getStatus(),
                user.getSuspendedUntil(),
                user.getSuspensionReason(),
                user.getCreatedAt(),
                accountNumMasked);
    }
}
