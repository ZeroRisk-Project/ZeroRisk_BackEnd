package com.zerorisk.project.domain.post.service;

import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.comment.repository.PostCommentCountProjection;
import com.zerorisk.project.domain.post.dto.PostCreateRequest;
import com.zerorisk.project.domain.post.dto.PostResponse;
import com.zerorisk.project.domain.post.dto.PostUpdateRequest;
import com.zerorisk.project.domain.post.entity.BoardType;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.exception.PostAccessDeniedException;
import com.zerorisk.project.global.exception.PostNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final UserActivityLogger userActivityLogger;

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
        userActivityLogger.log(userId, "POST_CREATE", "[" + request.boardType() + "] " + request.title());

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

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();

        // 게시글 목록 전체의 댓글 수를 쿼리 1번으로 집계 (기존 N+1 구조 개선)
        Map<Long, Integer> commentCountByPostId = commentRepository.countByPostIdsAndIsDeletedFalse(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentCountProjection::getPostId,
                        projection -> projection.getCommentCount().intValue()));

        return posts.map(post -> {
            int commentCount = commentCountByPostId.getOrDefault(post.getId(), 0);
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
        userActivityLogger.log(userId, "POST_UPDATE", "게시글 #" + postId + " 수정");

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
        userActivityLogger.log(userId, "POST_DELETE", "게시글 #" + postId + " 삭제");
    }
}