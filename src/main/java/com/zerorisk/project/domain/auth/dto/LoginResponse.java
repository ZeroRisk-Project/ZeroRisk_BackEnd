package com.zerorisk.project.domain.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String nickname) {
}