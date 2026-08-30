package com.zerorisk.project.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

// 주의: 이 이벤트 시점(STOMP CONNECT 완료 후)의 event.getUser()가 유일하게 신뢰 가능한 최종 확정 userId임.
// (StompChannelInterceptor가 CONNECT 헤더로 재인증한 경우까지 반영된 값)
// 세션 해제는 SessionBufferingDecoratorFactory.afterConnectionClosed에서 sessionId만으로 처리하므로 여기선 안 함.
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionEventListener {

    private final WebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UserPrincipal principal = (UserPrincipal) event.getUser();

        if (principal == null || accessor.getSessionId() == null) {
            return;
        }

        sessionRegistry.confirm(principal.getUserId(), accessor.getSessionId());
        log.debug("WebSocket 세션 확정 등록 - userId: {}, sessionId: {}", principal.getUserId(), accessor.getSessionId());
    }
}
