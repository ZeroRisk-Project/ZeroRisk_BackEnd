package com.zerorisk.project.domain.post.dto;

import com.zerorisk.project.domain.post.entity.BoardType;
import com.zerorisk.project.domain.post.entity.Post;
import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        BoardType boardType,
        Long authorId,
        String authorNickname,
        Integer authorLevel,
        Long stockId,
        String title,
        String content,
        boolean isProfitCert,
        String certImageUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isMine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostResponse from(Post post, int commentCount, Long viewerId) {
        boolean isMine = viewerId != null && post.getUser().getId().equals(viewerId);

        return new PostResponse(
                post.getId(),
                post.getBoardType(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                post.getUser().getUserLevel(),
                post.getStockId(),
                post.getTitle(),
                post.getContent(),
                post.getIsProfitCert(),
                post.getCertImageUrl(),
                post.getViewCount(),
                post.getLikeCount(),
                commentCount,
                isMine,
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
