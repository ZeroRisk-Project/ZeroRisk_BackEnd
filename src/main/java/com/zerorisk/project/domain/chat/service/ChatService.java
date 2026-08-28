package com.zerorisk.project.domain.chat.service;

import com.zerorisk.project.domain.chat.dto.ChatMessageResponse;
import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.domain.chat.repository.ChatMessageRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.UserNotFoundException;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse saveMessage(
            Long userId, ChatChannelType channelType, String channelId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        ChatMessage chatMessage = ChatMessage.builder()
                .channelType(channelType)
                .channelId(channelId)
                .user(user)
                .message(content)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        return ChatMessageResponse.from(savedMessage);
    }

    public Page<ChatMessageResponse> getMessages(
            ChatChannelType channelType, String channelId, Pageable pageable) {
        return chatMessageRepository
                .findByChannelTypeAndChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelType, channelId, pageable)
                .map(ChatMessageResponse::from);
    }
}
