package com.zerorisk.project.domain.comment.repository;

import com.zerorisk.project.domain.comment.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);

    long countByPostIdAndIsDeletedFalse(Long postId);

    @Query("SELECT c.id AS commentId, c.post.id AS postId FROM Comment c WHERE c.id IN :commentIds")
    List<CommentPostIdProjection> findPostIdsByCommentIds(@Param("commentIds") List<Long> commentIds);
}