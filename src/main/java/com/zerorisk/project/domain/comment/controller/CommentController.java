package com.zerorisk.project.domain.comment.controller;

import com.zerorisk.project.domain.comment.dto.CommentCreateRequest;
import com.zerorisk.project.domain.comment.dto.CommentResponse;
import com.zerorisk.project.domain.comment.dto.CommentUpdateRequest;
import com.zerorisk.project.domain.comment.service.CommentService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @CurrentUserId Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(userId, postId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @CurrentUserId Long viewerId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId, viewerId));
    }

    @PatchMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @CurrentUserId Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.updateComment(userId, commentId, request));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@CurrentUserId Long userId, @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);

        return ResponseEntity.noContent().build();
    }
}