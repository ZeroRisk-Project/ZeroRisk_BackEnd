package com.zerorisk.project.domain.moderation.service;

import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.moderation.dto.AdminCommentResponse;
import com.zerorisk.project.domain.moderation.dto.AdminPostResponse;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import com.zerorisk.project.global.exception.CommentNotFoundException;
import com.zerorisk.project.global.exception.PostNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AdminActionLogger adminActionLogger;

    // 관리자 게시글 목록 조회. 삭제된 글도 포함해서 전부 보여줌 (일반 유저 조회와의 핵심 차이)
    public Page<AdminPostResponse> getPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAllBy(pageable);

        return posts.map(post -> {
            int commentCount = (int) commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
            return AdminPostResponse.from(post, commentCount);
        });
    }

    public List<AdminCommentResponse> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(AdminCommentResponse::from)
                .toList();
    }

    @Transactional
    public void forceDeletePost(Long adminId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        post.softDelete();
        adminActionLogger.log(adminId, "FORCE_DELETE", "POST", postId, "게시글 강제 삭제");
    }

    @Transactional
    public void restorePost(Long adminId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        post.restore();
        adminActionLogger.log(adminId, "RESTORE", "POST", postId, "게시글 복구");
    }

    @Transactional
    public void forceDeleteComment(Long adminId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        comment.softDelete();
        adminActionLogger.log(adminId, "FORCE_DELETE", "COMMENT", commentId, "댓글 강제 삭제");
    }

    @Transactional
    public void restoreComment(Long adminId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        comment.restore();
        adminActionLogger.log(adminId, "RESTORE", "COMMENT", commentId, "댓글 복구");
    }
}
