package com.zerorisk.project.domain.post.dto;

import com.zerorisk.project.domain.post.entity.VoteType;
import jakarta.validation.constraints.NotNull;

public record PostVoteRequest(
        @NotNull VoteType voteType) {
}