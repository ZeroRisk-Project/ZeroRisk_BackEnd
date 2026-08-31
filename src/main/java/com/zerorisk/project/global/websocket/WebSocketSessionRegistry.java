package com.zerorisk.project.global.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class WebSocketSessionRegistry {

    // sessionId -> WebSocketSession. 핸드셰이크 직후 임시 보관 (아직 userId 확정 전).
    private final Map<String, WebSocketSession> pendingSessions = new ConcurrentHashMap<>();

    // userId -> (sessionId -> WebSocketSession). STOMP CONNECT 완료 후 최종 등록.
    private final Map<Long, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // 1단계: 핸드셰이크 직후, userId 확정 전 임시 보관
    public void holdPending(String sessionId, WebSocketSession session) {
        pendingSessions.put(sessionId, session);
    }

    // 2단계: STOMP CONNECT 완료 시, 확정된 userId로 정식 등록
    public void confirm(Long userId, String sessionId) {
        WebSocketSession session = pendingSessions.remove(sessionId);

        if (session == null) {
            return;
        }

        userSessions
                .computeIfAbsent(userId, id -> new ConcurrentHashMap<>())
                .put(sessionId, session);
    }

    // 연결 종료 시: pending이든 확정 등록이든 sessionId 하나로 전부 정리
    public void remove(String sessionId) {
        pendingSessions.remove(sessionId);

        for (Map<String, WebSocketSession> sessions : userSessions.values()) {
            sessions.remove(sessionId);
        }

        userSessions.values().removeIf(Map::isEmpty);
    }

    public boolean isConnected(Long userId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public int getSessionCount(Long userId) {
        Map<String, WebSocketSession> sessions = userSessions.get(userId);
        return sessions == null ? 0 : sessions.size();
    }

    // 강제 종료용: 확정 등록된 세션만 반환 (pending 상태 - 아직 인증 안 끝난 세션 - 는 대상 아님)
    public Map<String, WebSocketSession> getSessions(Long userId) {
        return userSessions.getOrDefault(userId, Map.of());
    }
}
