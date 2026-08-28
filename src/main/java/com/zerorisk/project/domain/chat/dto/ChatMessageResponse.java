package com.zerorisk.project.domain.chat.dto;

import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        ChatChannelType channelType,
        String channelId,
        Long authorId,
        String authorNickname,
        String message,
        LocalDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChannelType(),
                chatMessage.getChannelId(),
                chatMessage.getUser().getId(),
                chatMessage.getUser().getNickname(),
                chatMessage.getMessage(),
                chatMessage.getCreatedAt());
    }
}
