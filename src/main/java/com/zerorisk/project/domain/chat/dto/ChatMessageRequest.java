package com.zerorisk.project.domain.chat.dto;

import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @Size(max = 500) String message,
        String imageUrl) {
}
