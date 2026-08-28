package com.zerorisk.project.global.websocket;

import com.zerorisk.project.domain.chat.cache.CompetitionParticipantCache;
import com.zerorisk.project.global.exception.ChatAccessDeniedException;
import com.zerorisk.project.global.security.JwtTokenProvider;
import com.zerorisk.project.global.websocket.dto.ChatChannelType;
import com.zerorisk.project.global.websocket.ratelimit.ChatRateLimiter;
import com.zerorisk.project.global.websocket.status.UserStatusChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final long UNAUTHENTICATED_USER_ID = -1L;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRateLimiter chatRateLimiter;
    private final UserStatusChecker userStatusChecker;
    private final CompetitionParticipantCache competitionParticipantCache;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            return handleSend(message, accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        UserPrincipal principal = (UserPrincipal) accessor.getUser();

        if (principal != null && principal.getUserId() != UNAUTHENTICATED_USER_ID) {
            return;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("STOMP CONNECT 인증 실패 - Authorization 헤더 없음");
            throw new ChatAccessDeniedException("인증이 필요합니다.");
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            Long userId = jwtTokenProvider.getUserId(token);
            accessor.setUser(new UserPrincipal(userId));
        } catch (Exception e) {
            log.debug("STOMP CONNECT 헤더 토큰 검증 실패", e);
            throw new ChatAccessDeniedException("인증이 필요합니다.");
        }
    }

    // 구독(입장) 시점: 채널 접근 권한 검증. 여기서 막히면 연결 종료 대상.
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long userId = extractUserId(accessor);
        String destination = accessor.getDestination();

        ChannelInfo channelInfo = parseChannel(destination);

        if (channelInfo == null) {
            return;
        }

        validateChannelAccess(userId, channelInfo);
    }

    // 발행(메시지 전송) 시점: 정지 여부, Rate Limit, 길이 제한 검증
    private Message<?> handleSend(Message<?> message, StompHeaderAccessor accessor) {
        Long userId = extractUserId(accessor);
        String destination = accessor.getDestination();

        ChannelInfo channelInfo = parseChannel(destination);

        if (channelInfo == null) {
            return message;
        }

        // 정지 계정, 대회 자격 없음 -> 연결 종료 대상
        if (userStatusChecker.isSuspended(userId)) {
            throw new ChatAccessDeniedException("정지된 계정입니다.");
        }

        validateChannelAccess(userId, channelInfo);

        // Rate Limit 초과, 길이 초과 -> 메시지만 drop, 연결은 유지
        if (!chatRateLimiter.tryAcquire(userId)) {
            notifyError(userId, "메시지를 너무 빠르게 보내고 있습니다.");
            return null;
        }

        if (isMessageTooLong(message)) {
            notifyError(userId, "메시지는 500자를 초과할 수 없습니다.");
            return null;
        }

        return message;
    }

    // 채널 타입에 따른 접근 권한 검증. STOCK은 로그인만 하면 통과, COMPETITION은 참가자만 통과.
    private void validateChannelAccess(Long userId, ChannelInfo channelInfo) {
        if (channelInfo.channelType() == ChatChannelType.COMPETITION) {
            Long competitionId = Long.parseLong(channelInfo.channelId());

            if (!competitionParticipantCache.isParticipant(competitionId, userId)) {
                throw new ChatAccessDeniedException("대회 참가자만 입장할 수 있습니다.");
            }
        }
        // STOCK은 이 메서드에 도달했다는 것 자체가 이미 로그인된 상태(userId 확보)라는 뜻이라 별도 검증 없음
    }

    private boolean isMessageTooLong(Message<?> message) {
        Object payload = message.getPayload();

        if (payload instanceof byte[] bytes) {
            return bytes.length > MAX_MESSAGE_LENGTH * 3; // UTF-8 한글 최대 3바이트 고려한 대략적 상한
        }

        return false;
    }

    private void notifyError(Long userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), "/queue/errors", errorMessage);
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        UserPrincipal principal = (UserPrincipal) accessor.getUser();

        if (principal == null) {
            throw new ChatAccessDeniedException("인증 정보를 찾을 수 없습니다.");
        }

        return principal.getUserId();
    }

    // destination 형식: /topic/chat/{channelType}/{channelId} 또는 /app/chat/{channelType}/{channelId}
    private ChannelInfo parseChannel(String destination) {
        if (destination == null || !destination.contains("/chat/")) {
            return null;
        }

        String[] parts = destination.split("/chat/");

        if (parts.length < 2) {
            return null;
        }

        String[] channelParts = parts[1].split("/");

        if (channelParts.length < 2) {
            return null;
        }

        try {
            ChatChannelType channelType = ChatChannelType.valueOf(channelParts[0]);
            String channelId = channelParts[1];
            return new ChannelInfo(channelType, channelId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record ChannelInfo(ChatChannelType channelType, String channelId) {
    }
}
