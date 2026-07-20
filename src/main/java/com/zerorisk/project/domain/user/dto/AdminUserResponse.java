package com.zerorisk.project.domain.user.dto;

import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        UserStatus status,
        LocalDateTime suspendedUntil,
        String suspensionReason) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getSuspendedUntil(),
                user.getSuspensionReason());
    }
}