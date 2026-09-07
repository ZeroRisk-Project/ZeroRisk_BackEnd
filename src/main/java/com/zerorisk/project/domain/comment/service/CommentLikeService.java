package com.zerorisk.project.domain.comment.service;

import com.zerorisk.project.domain.comment.entity.Comment;
import com.zerorisk.project.domain.comment.entity.CommentLike;
import com.zerorisk.project.domain.comment.repository.CommentLikeRepository;
import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.CommentNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public void toggleLike(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(CommentNotFoundException::new);

        CommentLike existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);

        if (existingLike != null) {
            commentLikeRepository.delete(existingLike);
            comment.decreaseLikeCount();
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        CommentLike like = CommentLike.builder()
                .comment(comment)
                .user(user)
                .build();

        commentLikeRepository.save(like);
        comment.increaseLikeCount();
    }
}
