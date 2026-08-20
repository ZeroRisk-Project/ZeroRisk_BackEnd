package com.zerorisk.project.domain.moderation.service;

import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import com.zerorisk.project.global.exception.CommentNotFoundException;
import com.zerorisk.project.global.exception.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AdminActionLogger adminActionLogger;

    @Transactional
    public void forceDeletePost(Long adminId, Long postId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        post.softDelete();
        adminActionLogger.log(adminId, "FORCE_DELETE", "POST", postId, "게시글 강제 삭제");
    }

    @Transactional
    public void forceDeleteComment(Long adminId, Long commentId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(CommentNotFoundException::new);

        comment.softDelete();
        adminActionLogger.log(adminId, "FORCE_DELETE", "COMMENT", commentId, "댓글 강제 삭제");
    }
}