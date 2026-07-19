package com.zerorisk.project.domain.post.repository;

import com.zerorisk.project.domain.post.entity.BoardType;
import com.zerorisk.project.domain.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndIsDeletedFalse(Long id);

    Page<Post> findByBoardTypeAndIsDeletedFalse(BoardType boardType, Pageable pageable);

    Page<Post> findByIsDeletedFalse(Pageable pageable);
}