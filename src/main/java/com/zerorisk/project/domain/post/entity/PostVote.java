package com.zerorisk.project.domain.post.entity;

import com.zerorisk.project.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "POST_VOTES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_vote_seq")
    @SequenceGenerator(name = "post_vote_seq", sequenceName = "POST_VOTES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "VOTE_TYPE", nullable = false, length = 10)
    private VoteType voteType;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PostVote(Post post, User user, VoteType voteType) {
        this.post = post;
        this.user = user;
        this.voteType = voteType;
        this.createdAt = LocalDateTime.now();
    }

    public void changeType(VoteType voteType) {
        this.voteType = voteType;
    }
}