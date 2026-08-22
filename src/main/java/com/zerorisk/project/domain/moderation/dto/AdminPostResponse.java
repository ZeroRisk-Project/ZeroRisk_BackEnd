package com.zerorisk.project.domain.moderation.dto;

import com.zerorisk.project.domain.post.entity.Post;
import java.time.LocalDateTime;

public record AdminPostResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime date,
        int views,
        int likes,
        int commentsCount,
        String status) {

    public static AdminPostResponse from(Post post, int commentCount) {
        return new AdminPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getNickname(),
                post.getCreatedAt(),
                post.getViewCount(),
                post.getLikeCount(),
                commentCount,
                post.getIsDeleted() ? "DELETED" : "ACTIVE");
    }
}
