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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 주의: (POST_ID, USER_ID) UNIQUE 제약이 DDL에 없어서 동시 요청 시 중복 행 가능성 있음.
// DB 제약 추가 전까지는 이 로직이 유일한 방어선.
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

        postVoteRepository.save(vote);

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