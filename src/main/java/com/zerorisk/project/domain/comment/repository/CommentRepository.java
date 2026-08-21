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

    // 게시글 목록 조회 시 N+1 방지용: 여러 게시글의 댓글 수를 GROUP BY로 한 번에 집계
    @Query("SELECT c.post.id AS postId, COUNT(c) AS commentCount "
            + "FROM Comment c "
            + "WHERE c.post.id IN :postIds AND c.isDeleted = false "
            + "GROUP BY c.post.id")
    List<PostCommentCountProjection> countByPostIdsAndIsDeletedFalse(@Param("postIds") List<Long> postIds);
}