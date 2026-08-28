package com.zerorisk.project.domain.chat.repository;

import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndIsDeletedFalse(Long id);

    Page<ChatMessage> findByChannelTypeAndChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
            ChatChannelType channelType, String channelId, Pageable pageable);
}
