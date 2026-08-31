package com.zerorisk.project.domain.chat.entity;

import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHAT_MESSAGES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_message_seq")
    @SequenceGenerator(name = "chat_message_seq", sequenceName = "CHAT_MESSAGES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL_TYPE", nullable = false, length = 15)
    private ChatChannelType channelType;

    // 종목 채팅방은 종목코드(예: "005930"), 대회 채팅방은 대회 ID를 문자열로 저장
    @Column(name = "CHANNEL_ID", nullable = false, length = 50)
    private String channelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Lob
    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "IS_DELETED", nullable = false)
    private Boolean isDeleted;

    @Column(name = "IS_REPORTED", nullable = false)
    private Boolean isReported;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatMessage(ChatChannelType channelType, String channelId, User user, String message) {
        this.channelType = channelType;
        this.channelId = channelId;
        this.user = user;
        this.message = message;
        this.isDeleted = false;
        this.isReported = false;
        this.createdAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void markAsReported() {
        this.isReported = true;
    }

    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }
}
