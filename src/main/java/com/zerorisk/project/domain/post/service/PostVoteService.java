package com.zerorisk.project.domain.post.service;

import com.zerorisk.project.domain.post.dto.PostVoteRequest;
import com.zerorisk.project.domain.post.entity.Post;
import com.zerorisk.project.domain.post.entity.PostVote;
import com.zerorisk.project.domain.post.entity.VoteType;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.post.repository.PostVoteRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.PostNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// (POST_ID, USER_ID) UNIQUE 제약을 DB에 추가함 - 동시 요청으로 두 트랜잭션이 모두 "투표 없음"으로
// 판단해 동시에 insert를 시도하면, 나중에 flush되는 쪽이 제약 위반을 받는다. createVote()에서
// saveAndFlush + catch로 그 경우를 조용히 무시(이미 다른 요청이 처리함)하도록 방어한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PostVoteService {

    private final PostVoteRepository postVoteRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void vote(Long userId, Long postId, PostVoteRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(PostNotFoundException::new);

        PostVote existingVote = postVoteRepository.findByPostIdAndUserId(postId, userId).orElse(null);

        if (existingVote == null) {
            createVote(post, userId, request.voteType());
            return;
        }

        if (existingVote.getVoteType() == request.voteType()) {
            cancelVote(existingVote, post);
            return;
        }

        changeVote(existingVote, post, request.voteType());
    }

    private void createVote(Post post, Long userId, VoteType voteType) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        PostVote vote = PostVote.builder()
                .post(post)
                .user(user)
                .voteType(voteType)
                .build();

        try {
            postVoteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException e) {
            log.info("게시글 {} 투표 동시 생성 감지 - userId: {}, 이미 처리된 것으로 보고 무시합니다.", post.getId(), userId);
            return;
        }

        increaseCount(post, voteType);
    }

    private void cancelVote(PostVote existingVote, Post post) {
        decreaseCount(post, existingVote.getVoteType());

        postVoteRepository.delete(existingVote);
    }

    private void changeVote(PostVote existingVote, Post post, VoteType newVoteType) {
        decreaseCount(post, existingVote.getVoteType());
        increaseCount(post, newVoteType);

        existingVote.changeType(newVoteType);
    }

    private void increaseCount(Post post, VoteType voteType) {
        if (voteType == VoteType.LIKE) {
            post.increaseLikeCount();
        } else {
            post.increaseDislikeCount();
        }
    }

    private void decreaseCount(Post post, VoteType voteType) {
        if (voteType == VoteType.LIKE) {
            post.decreaseLikeCount();
        } else {
            post.decreaseDislikeCount();
        }
    }
}