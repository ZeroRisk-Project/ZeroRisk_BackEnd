package com.zerorisk.project.domain.comment.dto;

import com.zerorisk.project.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorNickname,
        Long parentId,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt,
        List<CommentResponse> replies) {

    public static CommentResponse from(Comment comment) {
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        return new CommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                parentId,
                comment.getIsDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.getIsDeleted(),
                comment.getCreatedAt(),
                new ArrayList<>());
    }
}