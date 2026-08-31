package com.zerorisk.project.domain.chat.repository;

import com.zerorisk.project.domain.chat.entity.ChatMessage;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndIsDeletedFalse(Long id);

    Page<ChatMessage> findByChannelTypeAndChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
            ChatChannelType channelType, String channelId, Pageable pageable);

    // 종목 채팅 보관 정책 배치용: N일 지난 미삭제·미신고 종목 메시지
    List<ChatMessage> findByChannelTypeAndIsDeletedFalseAndIsReportedFalseAndCreatedAtBefore(
            ChatChannelType channelType, LocalDateTime cutoff);

    // 대회 채팅 보관 정책 배치용: 특정 대회(channelId)의 미삭제·미신고 메시지 전체
    List<ChatMessage> findByChannelTypeAndChannelIdAndIsDeletedFalseAndIsReportedFalse(
            ChatChannelType channelType, String channelId);
}
