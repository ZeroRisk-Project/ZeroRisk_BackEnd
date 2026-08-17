package com.zerorisk.project.domain.profile.service;

import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.PrizeHistoryRepository;
import com.zerorisk.project.domain.competition.repository.ProfileCompetitionProjection;
import com.zerorisk.project.domain.follow.repository.FollowRepository;
import com.zerorisk.project.domain.profile.dto.ProfileResponse;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final PrizeHistoryRepository prizeHistoryRepository;

    public ProfileResponse getProfile(Long targetUserId, Long viewerId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        long followerCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        boolean isMe = viewerId != null && viewerId.equals(targetUserId);
        boolean isFollowing = viewerId != null && !isMe
                && followRepository.findByFollowerIdAndFollowingId(viewerId, targetUserId).isPresent();

        return new ProfileResponse(
                target.getId(), target.getNickname(), target.getProfileImageUrl(),
                target.getUserLevel(), target.getActivityScore(), target.getCreatedAt(),
                followerCount, followingCount, isFollowing, isMe,
                getCompetitionHistory(targetUserId));
    }

    private List<ProfileResponse.ProfileCompetitionHistory> getCompetitionHistory(Long userId) {
        List<ProfileCompetitionProjection> history = competitionParticipantRepository.findCompetitionHistoryByUserId(userId);

        Map<Long, BigDecimal> prizeMap = prizeHistoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(PrizeHistory::getCompetitionId, PrizeHistory::getPrizeAmount));

        return history.stream()
                .map(h -> new ProfileResponse.ProfileCompetitionHistory(
                        h.getCompetitionId(), h.getTitle(), h.getRankPosition(),
                        h.getReturnRate(), prizeMap.get(h.getCompetitionId())))
                .toList();
    }
}
