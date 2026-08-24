package com.zerorisk.project.domain.profile.service;

import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.PrizeHistoryRepository;
import com.zerorisk.project.domain.competition.repository.ProfileCompetitionProjection;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.follow.repository.FollowRepository;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.profile.dto.ProfileResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsResponse;
import com.zerorisk.project.domain.profile.dto.ProfileSettingsUpdateRequest;
import com.zerorisk.project.domain.profile.entity.ProfileSettings;
import com.zerorisk.project.domain.profile.repository.ProfileSettingsRepository;
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
    private final ProfileSettingsRepository profileSettingsRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // findOrCreateSettings가 최초 조회 시 로우를 새로 INSERT할 수 있어 readOnly 트랜잭션을 쓰면 안 됨
    @Transactional
    public ProfileResponse getProfile(Long targetUserId, Long viewerId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        ProfileSettings settings = findOrCreateSettings(targetUserId);

        long followerCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        boolean isMe = viewerId != null && viewerId.equals(targetUserId);
        boolean isFollowing = viewerId != null && !isMe
                && followRepository.findByFollowerIdAndFollowingId(viewerId, targetUserId).isPresent();

        // 본인이 보거나, 공개 설정이 켜져 있으면 보여줌
        boolean canSeeCompetitions = isMe || settings.getShowCompetitions();

        long postCount = postRepository.countByUser_IdAndIsDeletedFalse(targetUserId);
        long commentCount = commentRepository.countByUser_IdAndIsDeletedFalse(targetUserId);

        return new ProfileResponse(
                target.getId(), target.getNickname(), target.getProfileImageUrl(),
                target.getUserLevel(), target.getActivityScore(), target.getCreatedAt(),
                followerCount, followingCount, isFollowing, isMe, postCount, commentCount,
                canSeeCompetitions ? getCompetitionHistory(targetUserId) : List.of());
    }

    @Transactional
    public ProfileSettingsResponse getMySettings(Long userId) {
        ProfileSettings settings = findOrCreateSettings(userId);
        return new ProfileSettingsResponse(
                settings.getShowReturnRate(), settings.getShowPortfolio(), settings.getShowTrades(),
                settings.getShowStats(), settings.getShowCompetitions());
    }

    @Transactional
    public ProfileSettingsResponse updateMySettings(Long userId, ProfileSettingsUpdateRequest request) {
        ProfileSettings settings = findOrCreateSettings(userId);
        settings.update(
                request.showReturnRate(), request.showPortfolio(), request.showTrades(),
                request.showStats(), request.showCompetitions());
        return new ProfileSettingsResponse(
                settings.getShowReturnRate(), settings.getShowPortfolio(), settings.getShowTrades(),
                settings.getShowStats(), settings.getShowCompetitions());
    }

    private ProfileSettings findOrCreateSettings(Long userId) {
        return profileSettingsRepository.findByUserId(userId)
                .orElseGet(() -> profileSettingsRepository.save(
                        ProfileSettings.builder().userId(userId).build()));
    }

    private List<ProfileResponse.ProfileCompetitionHistory> getCompetitionHistory(Long userId) {
        List<ProfileCompetitionProjection> history = competitionParticipantRepository.findCompetitionHistoryByUserId(userId);

        Map<Long, BigDecimal> prizeMap = prizeHistoryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(PrizeHistory::getCompetitionId, PrizeHistory::getPrizeAmount));

        return history.stream()
                .map(h -> new ProfileResponse.ProfileCompetitionHistory(
                        h.getCompetitionId(), h.getTitle(), h.getStartAt(), h.getEndAt(),
                        h.getSeedMoney(), h.getStatus(), h.getRankPosition(),
                        h.getReturnRate(), prizeMap.get(h.getCompetitionId())))
                .toList();
    }
}
