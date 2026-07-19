package com.zerorisk.project.domain.follow.controller;

import com.zerorisk.project.domain.follow.dto.FollowUserResponse;
import com.zerorisk.project.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> follow(@PathVariable Long userId) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 followerId 주입
        Long followerId = 1L;

        followService.follow(followerId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 followerId 주입
        Long followerId = 1L;

        followService.unfollow(followerId, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<FollowUserResponse>> getFollowers(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/followings")
    public ResponseEntity<Page<FollowUserResponse>> getFollowings(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowings(userId, pageable));
    }
}