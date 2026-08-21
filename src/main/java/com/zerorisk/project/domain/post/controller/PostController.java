package com.zerorisk.project.domain.post.controller;

import com.zerorisk.project.domain.post.dto.PostCreateRequest;
import com.zerorisk.project.domain.post.dto.PostResponse;
import com.zerorisk.project.domain.post.dto.PostUpdateRequest;
import com.zerorisk.project.domain.post.dto.PostVoteRequest;
import com.zerorisk.project.domain.post.entity.BoardType;
import com.zerorisk.project.domain.post.service.PostService;
import com.zerorisk.project.domain.post.service.PostVoteService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostVoteService postVoteService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @CurrentUserId Long userId,
            @Valid @RequestBody PostCreateRequest request) {
        PostResponse response = postService.createPost(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @CurrentUserId Long viewerId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId, viewerId));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @CurrentUserId Long viewerId,
            @RequestParam(required = false) BoardType boardType,
            Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(boardType, pageable, viewerId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(postService.updatePost(userId, postId, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@CurrentUserId Long userId, @PathVariable Long postId) {
        postService.deletePost(userId, postId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/votes")
    public ResponseEntity<Void> votePost(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostVoteRequest request) {
        postVoteService.vote(userId, postId, request);

        return ResponseEntity.noContent().build();
    }
}