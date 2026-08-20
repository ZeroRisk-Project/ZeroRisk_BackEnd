package com.zerorisk.project.domain.follow.service;

import com.zerorisk.project.domain.follow.dto.FollowUserResponse;
import com.zerorisk.project.domain.follow.entity.Follow;
import com.zerorisk.project.domain.follow.repository.FollowRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.DuplicateFollowException;
import com.zerorisk.project.global.exception.FollowNotFoundException;
import com.zerorisk.project.global.exception.SelfFollowException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import com.zerorisk.project.global.audit.UserActivityLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserActivityLogger userActivityLogger;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new SelfFollowException();
        }

        if (followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent()) {
            throw new DuplicateFollowException();
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(UserNotFoundException::new);
        User following = userRepository.findById(followingId)
                .orElseThrow(UserNotFoundException::new);

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        followRepository.save(follow);
        userActivityLogger.log(followerId, "FOLLOW", "유저 #" + followingId + " 팔로우");
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(FollowNotFoundException::new);

        followRepository.delete(follow);
        userActivityLogger.log(followerId, "UNFOLLOW", "유저 #" + followingId + " 언팔로우");
    }

    public Page<FollowUserResponse> getFollowers(Long userId, Pageable pageable) {
        return followRepository.findByFollowingId(userId, pageable)
                .map(follow -> FollowUserResponse.from(follow.getFollower()));
    }

    public Page<FollowUserResponse> getFollowings(Long userId, Pageable pageable) {
        return followRepository.findByFollowerId(userId, pageable)
                .map(follow -> FollowUserResponse.from(follow.getFollowing()));
    }
}
