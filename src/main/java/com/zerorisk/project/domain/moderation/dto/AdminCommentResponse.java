package com.zerorisk.project.domain.moderation.dto;

import com.zerorisk.project.domain.comment.entity.Comment;
import java.time.LocalDateTime;

public record AdminCommentResponse(
        Long id,
        String content,
        String author,
        boolean isDeleted,
        LocalDateTime createdAt) {

    public static AdminCommentResponse from(Comment comment) {
        return new AdminCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getNickname(),
                comment.getIsDeleted(),
                comment.getCreatedAt());
    }
}
