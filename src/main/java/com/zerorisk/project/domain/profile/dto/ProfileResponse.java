package com.zerorisk.project.domain.profile.dto;

import com.zerorisk.project.domain.competition.entity.CompetitionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        Integer userLevel,
        Integer activityScore,
        LocalDateTime createdAt,
        long followerCount,
        long followingCount,
        boolean isFollowing,
        boolean isMe,
        List<ProfileCompetitionHistory> competitionHistory) {

    public record ProfileCompetitionHistory(
            Long competitionId,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal seedMoney,
            CompetitionStatus status,
            Integer rankPosition,
            BigDecimal returnRate,
            BigDecimal prizeAmount) {
    }
}
