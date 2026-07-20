package com.zerorisk.project.domain.moderation.service;

import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.repository.PostRepository;
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

    @Transactional
    public void forceDeletePost(Long postId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        post.softDelete();
    }

    @Transactional
    public void forceDeleteComment(Long commentId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(CommentNotFoundException::new);

        comment.softDelete();
    }
}