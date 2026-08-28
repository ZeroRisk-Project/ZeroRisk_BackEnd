package com.zerorisk.project.global.websocket;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        Object userId = attributes.get(CustomHandshakeInterceptor.USER_ID_ATTRIBUTE);

        if (userId instanceof Long id) {
            return new UserPrincipal(id);
        }

        // 핸드셰이크 단계에서 인증 실패 - STOMP CONNECT 헤더 단계에서 재검증하도록 임시 익명 Principal 부여
        return new UserPrincipal(-1L);
    }
}
