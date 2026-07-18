package com.zerorisk.project.domain.user.dto;

public record SignupResponse(
        Long userId,
        String email,
        String nickname) {
}