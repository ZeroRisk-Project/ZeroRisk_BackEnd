package com.zerorisk.project.domain.post.repository;

import com.zerorisk.project.domain.post.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Long postId);

    // 게시글 목록 조회 시 N+1 방지용: 여러 게시글의 이미지를 한 번에 조회
    List<PostImage> findByPostIdInOrderByDisplayOrderAsc(List<Long> postIds);

    void deleteByPostId(Long postId);
}
