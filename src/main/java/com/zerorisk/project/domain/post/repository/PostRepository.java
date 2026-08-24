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

    // 관리자 전용: 삭제된 게시글도 포함해서 전체 조회 (복구 대상 확인용)
    Page<Post> findAllBy(Pageable pageable);

    long countByUser_IdAndIsDeletedFalse(Long userId);
}