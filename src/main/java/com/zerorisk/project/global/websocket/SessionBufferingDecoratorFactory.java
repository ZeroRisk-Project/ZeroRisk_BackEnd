package com.zerorisk.project.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionBufferingDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                // 이 시점엔 아직 STOMP CONNECT 전이라 진짜 userId를 신뢰할 수 없음(핸드셰이크 실패 시 -1L일 수 있음).
                // 그래서 확정 등록은 안 하고, sessionId 기준으로만 세션 객체를 임시 보관해둠.
                // 실제 등록은 WebSocketSessionEventListener가 SessionConnectedEvent에서 확정된 userId로 처리.
                sessionRegistry.holdPending(session.getId(), session);
                log.debug("WebSocket raw 세션 임시 보관 - sessionId: {}", session.getId());
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                // userId를 몰라도 sessionId만으로 pending/확정 등록 전부에서 제거 가능
                sessionRegistry.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
}
