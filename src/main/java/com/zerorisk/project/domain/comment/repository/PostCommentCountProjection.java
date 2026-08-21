package com.zerorisk.project.domain.comment.repository;

public interface PostCommentCountProjection {
    Long getPostId();
    Long getCommentCount();
}
