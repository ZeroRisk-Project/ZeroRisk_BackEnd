package com.zerorisk.project.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.global.websocket.dto.DisconnectEventDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDisconnectSubscriber implements MessageListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            DisconnectEventDto event = objectMapper.readValue(message.getBody(), DisconnectEventDto.class);
            closeLocalSessions(event.getUserId(), event.getReason());
        } catch (Exception e) {
            log.warn("강제 종료 이벤트 처리 실패", e);
        }
    }

    private void closeLocalSessions(Long userId, String reason) {
        Map<String, WebSocketSession> sessions = sessionRegistry.getSessions(userId);

        if (sessions.isEmpty()) {
            // 이 서버 인스턴스엔 해당 유저 세션이 없음 - 다른 인스턴스에 연결돼 있거나 이미 끊긴 상태. 정상.
            return;
        }

        for (WebSocketSession session : sessions.values()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
                log.info("WebSocket 세션 강제 종료 - userId: {}, sessionId: {}, reason: {}",
                        userId, session.getId(), reason);
            } catch (Exception e) {
                log.warn("세션 close 실패 - userId: {}, sessionId: {}", userId, session.getId(), e);
            }
        }
    }
}
