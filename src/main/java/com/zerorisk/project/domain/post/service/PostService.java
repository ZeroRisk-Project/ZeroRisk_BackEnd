package com.zerorisk.project.domain.post.service;

import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.post.dto.PostCreateRequest;
import com.zerorisk.project.domain.post.dto.PostResponse;
import com.zerorisk.project.domain.post.dto.PostUpdateRequest;
import com.zerorisk.project.domain.post.entity.BoardType;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.PostAccessDeniedException;
import com.zerorisk.project.global.exception.PostNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Post post = Post.builder()
                .boardType(request.boardType())
                .user(user)
                .stockId(request.stockId())
                .title(request.title())
                .content(request.content())
                .build();

        Post savedPost = postRepository.save(post);

        // 방금 만든 글이라 댓글이 있을 수 없으므로 0 고정
        return PostResponse.from(savedPost, 0);
    }

    @Transactional
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        post.increaseViewCount();

        int commentCount = (int) commentRepository.countByPostIdAndIsDeletedFalse(postId);

        return PostResponse.from(post, commentCount);
    }

    public Page<PostResponse> getPosts(BoardType boardType, Pageable pageable) {
        Page<Post> posts = boardType != null
                ? postRepository.findByBoardTypeAndIsDeletedFalse(boardType, pageable)
                : postRepository.findByIsDeletedFalse(pageable);

        // 주의: 게시글마다 댓글 수 쿼리를 따로 날리는 N+1 구조.
        // 지금은 동작 우선으로 두고, 트래픽 늘면 서브쿼리로 한 번에 가져오는 최적화 필요.
        return posts.map(post -> {
            int commentCount = (int) commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
            return PostResponse.from(post, commentCount);
        });
    }

    @Transactional
    public PostResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        if (!post.isOwner(userId)) {
            throw new PostAccessDeniedException();
        }

        post.update(request.title(), request.content());

        int commentCount = (int) commentRepository.countByPostIdAndIsDeletedFalse(postId);

        return PostResponse.from(post, commentCount);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        if (!post.isOwner(userId)) {
            throw new PostAccessDeniedException();
        }

        post.softDelete();
    }
}