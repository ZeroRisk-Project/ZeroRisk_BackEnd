package com.zerorisk.project.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequest(
        @NotBlank String content) {
}