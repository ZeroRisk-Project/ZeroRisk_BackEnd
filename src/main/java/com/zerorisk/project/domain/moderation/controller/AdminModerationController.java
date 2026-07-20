package com.zerorisk.project.domain.moderation.controller;

import com.zerorisk.project.domain.moderation.service.AdminModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: SecurityConfig에서 /api/v1/admin/** 경로에 ADMIN 권한 체크 적용 필요 (박종권님 설정 확인)
@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    // 신고 접수된 게시글을 관리자가 강제로 소프트 삭제 (작성자 본인 확인 없이 바로 처리)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> forceDeletePost(@PathVariable Long postId) {
        adminModerationService.forceDeletePost(postId);

        return ResponseEntity.noContent().build();
    }

    // 신고 접수된 댓글을 관리자가 강제로 소프트 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> forceDeleteComment(@PathVariable Long commentId) {
        adminModerationService.forceDeleteComment(commentId);

        return ResponseEntity.noContent().build();
    }
}