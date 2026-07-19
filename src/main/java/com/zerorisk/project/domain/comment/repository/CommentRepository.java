package com.zerorisk.project.domain.comment.repository;

import com.zerorisk.project.domain.comment.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}