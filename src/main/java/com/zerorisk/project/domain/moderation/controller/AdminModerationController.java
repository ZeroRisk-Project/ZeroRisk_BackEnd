package com.zerorisk.project.domain.moderation.controller;

import com.zerorisk.project.domain.moderation.service.AdminModerationService;
import com.zerorisk.project.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    // 신고 접수된 게시글을 관리자가 강제로 소프트 삭제 (작성자 본인 확인 없이 바로 처리)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> forceDeletePost(@CurrentUserId Long adminId, @PathVariable Long postId) {
        adminModerationService.forceDeletePost(adminId, postId);

        return ResponseEntity.noContent().build();
    }

    // 신고 접수된 댓글을 관리자가 강제로 소프트 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> forceDeleteComment(@CurrentUserId Long adminId, @PathVariable Long commentId) {
        adminModerationService.forceDeleteComment(adminId, commentId);

        return ResponseEntity.noContent().build();
    }
}