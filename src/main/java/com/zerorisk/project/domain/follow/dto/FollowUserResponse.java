package com.zerorisk.project.domain.follow.dto;

import com.zerorisk.project.domain.user.entity.User;

public record FollowUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl) {

    public static FollowUserResponse from(User user) {
        return new FollowUserResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl());
    }
}