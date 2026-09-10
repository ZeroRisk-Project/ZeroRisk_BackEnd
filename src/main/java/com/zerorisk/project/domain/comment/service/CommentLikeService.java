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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// (COMMENT_ID, USER_ID) UNIQUE 제약을 DB에 추가함 - 동시 클릭으로 두 트랜잭션이 모두 "좋아요 없음"으로
// 판단해 동시에 insert를 시도하면, 나중에 flush되는 쪽이 제약 위반을 받는다. saveAndFlush + catch로
// 그 경우를 조용히 무시(이미 다른 요청이 처리함)하도록 방어한다.
@Slf4j
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

        try {
            commentLikeRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException e) {
            log.info("댓글 {} 좋아요 동시 생성 감지 - userId: {}, 이미 처리된 것으로 보고 무시합니다.", commentId, userId);
            return;
        }
        comment.increaseLikeCount();
    }
}
