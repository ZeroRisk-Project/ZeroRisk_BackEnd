package com.zerorisk.project.domain.chat.service;

import com.zerorisk.project.domain.chat.dto.ChatMessageResponse;
import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.domain.chat.repository.ChatMessageRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionRepository;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.ChatAccessDeniedException;
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
    private final StockRepository stockRepository;
    private final CompetitionRepository competitionRepository;

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
        validateChannel(channelType, channelId);

        return chatMessageRepository
                .findByChannelTypeAndChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        channelType, channelId, pageable)
                .map(ChatMessageResponse::from);
    }

    // WebSocket(StompChannelInterceptor)의 SUBSCRIBE 검증과 일관되게, REST 히스토리 조회도 존재하지 않는 채널이면 거부
    private void validateChannel(ChatChannelType channelType, String channelId) {
        if (channelType == ChatChannelType.STOCK && stockRepository.findByCode(channelId).isEmpty()) {
            throw new ChatAccessDeniedException("존재하지 않는 종목입니다.");
        }

        // 히스토리 조회는 참가자가 아니어도 가능(종료된 대회 다시보기 목적) - 대회 자체 존재 여부만 검증
        if (channelType == ChatChannelType.COMPETITION
                && !competitionRepository.existsById(Long.parseLong(channelId))) {
            throw new ChatAccessDeniedException("존재하지 않는 대회입니다.");
        }
    }
}
