package com.zerorisk.project.domain.chat.controller;

import com.zerorisk.project.domain.chat.dto.ChatMessageRequest;
import com.zerorisk.project.domain.chat.dto.ChatMessageResponse;
import com.zerorisk.project.domain.chat.service.ChatService;
import com.zerorisk.project.global.websocket.UserPrincipal;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/{channelType}/{channelId}")
    @SendTo("/topic/chat/{channelType}/{channelId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable ChatChannelType channelType,
            @DestinationVariable String channelId,
            @Valid ChatMessageRequest request,
            UserPrincipal principal) {
        return chatService.saveMessage(principal.getUserId(), channelType, channelId, request.message());
    }
}
