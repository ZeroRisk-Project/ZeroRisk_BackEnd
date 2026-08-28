package com.zerorisk.project.global.websocket;

import com.zerorisk.project.global.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            Long userId = extractUserId(servletRequest.getServletRequest());

            if (userId != null) {
                attributes.put(USER_ID_ATTRIBUTE, userId);
            }
        }

        // 여기서 실패해도 handshake는 통과시킴 - STOMP CONNECT 헤더 단계에서 재검증
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // 별도 처리 없음
    }

    private Long extractUserId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                try {
                    return jwtTokenProvider.getUserId(cookie.getValue());
                } catch (Exception e) {
                    log.debug("핸드셰이크 쿠키 토큰 검증 실패", e);
                    return null;
                }
            }
        }

        return null;
    }
}
