package com.zerorisk.project.domain.comment.dto;

import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.post.entity.BoardType;
import java.time.LocalDateTime;

public record MyCommentResponse(
        Long id,
        Long postId,
        String postTitle,
        BoardType boardType,
        String content,
        int likeCount,
        LocalDateTime createdAt) {

    public static MyCommentResponse from(Comment comment) {
        return new MyCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getPost().getBoardType(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt());
    }
}
