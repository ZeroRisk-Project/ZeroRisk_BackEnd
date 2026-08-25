package com.zerorisk.project.domain.user.dto;

public record MyProfileResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Integer activityScore,
        Integer userLevel,
        String userRole,
        String oauthProvider,
        Boolean hasClaimedPracticeCredit) {
    public static MyProfileResponse from(com.zerorisk.project.domain.user.entity.User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getActivityScore(),
                user.getUserLevel(),
                user.getUserRole().name(),
                user.getOauthProvider() != null ? user.getOauthProvider().name() : null,
                user.getHasClaimedPracticeCredit());
    }
}