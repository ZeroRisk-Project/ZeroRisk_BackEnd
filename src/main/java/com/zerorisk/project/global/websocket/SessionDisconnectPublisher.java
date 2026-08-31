package com.zerorisk.project.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.global.websocket.dto.DisconnectEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionDisconnectPublisher {

    public static final String CHANNEL = "ws:command:disconnect";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, String reason) {
        try {
            DisconnectEventDto event = DisconnectEventDto.builder()
                    .userId(userId)
                    .reason(reason)
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, payload);
        } catch (Exception e) {
            log.warn("강제 종료 이벤트 발행 실패 - userId: {}", userId, e);
        }
    }
}
