package com.zerorisk.project.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CustomHandshakeInterceptor customHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;
    private final StompChannelInterceptor stompChannelInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(customHandshakeInterceptor)
                .setHandshakeHandler(customHandshakeHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 단일 인스턴스 단계: 인메모리 SimpleBroker.
        // 스케일아웃 시 설계 문서 2-8의 Redis Pub/Sub 릴레이 패턴으로 전환 필요.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
        registration.taskExecutor()
                .corePoolSize(Runtime.getRuntime().availableProcessors() * 2)
                .maxPoolSize(20)
                .queueCapacity(1000)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(Runtime.getRuntime().availableProcessors() * 2)
                .maxPoolSize(20)
                .queueCapacity(1000)
                .keepAliveSeconds(60);
    }
}
