package com.zerorisk.project.domain.moderation.controller;

import com.zerorisk.project.domain.moderation.dto.AdminCommentResponse;
import com.zerorisk.project.domain.moderation.dto.AdminPostResponse;
import com.zerorisk.project.domain.moderation.service.AdminModerationService;
import com.zerorisk.project.global.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    // 관리자 게시글 관리 목록. 삭제 여부와 무관하게 전체 조회 (검색/상태필터는 프론트에서 처리)
    @GetMapping("/posts")
    public Page<AdminPostResponse> getPosts(Pageable pageable) {
        return adminModerationService.getPosts(pageable);
    }

    @GetMapping("/posts/{postId}/comments")
    public List<AdminCommentResponse> getComments(@PathVariable Long postId) {
        return adminModerationService.getComments(postId);
    }

    // 신고 접수된 게시글을 관리자가 강제로 소프트 삭제 (작성자 본인 확인 없이 바로 처리)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> forceDeletePost(@CurrentUserId Long adminId, @PathVariable Long postId) {
        adminModerationService.forceDeletePost(adminId, postId);

        return ResponseEntity.noContent().build();
    }

    // 삭제된 게시글 복구
    @PatchMapping("/posts/{postId}/restore")
    public ResponseEntity<Void> restorePost(@CurrentUserId Long adminId, @PathVariable Long postId) {
        adminModerationService.restorePost(adminId, postId);

        return ResponseEntity.noContent().build();
    }

    // 신고 접수된 댓글을 관리자가 강제로 소프트 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> forceDeleteComment(@CurrentUserId Long adminId, @PathVariable Long commentId) {
        adminModerationService.forceDeleteComment(adminId, commentId);

        return ResponseEntity.noContent().build();
    }

    // 삭제된 댓글 복구
    @PatchMapping("/comments/{commentId}/restore")
    public ResponseEntity<Void> restoreComment(@CurrentUserId Long adminId, @PathVariable Long commentId) {
        adminModerationService.restoreComment(adminId, commentId);

        return ResponseEntity.noContent().build();
    }
}
