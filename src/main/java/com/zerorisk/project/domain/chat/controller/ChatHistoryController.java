package com.zerorisk.project.domain.chat.controller;

import com.zerorisk.project.domain.chat.dto.ChatMessageResponse;
import com.zerorisk.project.domain.chat.service.ChatService;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public Page<ChatMessageResponse> getMessages(
            @RequestParam ChatChannelType channelType,
            @RequestParam String channelId,
            Pageable pageable) {
        return chatService.getMessages(channelType, channelId, pageable);
    }
}
