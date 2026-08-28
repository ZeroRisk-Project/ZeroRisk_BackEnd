package com.zerorisk.project.global.websocket.ratelimit;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private static final int WINDOW_SECONDS = 3;
    private static final int MAX_MESSAGES_PER_WINDOW = 5;
    private static final String KEY_PREFIX = "chat:ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript = createScript();

    private DefaultRedisScript<Long> createScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate_limit.lua"));
        script.setResultType(Long.class);
        return script;
    }

    // true = 허용, false = 제한 초과
    public boolean tryAcquire(Long userId) {
        String key = KEY_PREFIX + userId;

        try {
            Long count = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(WINDOW_SECONDS));

            return count == null || count <= MAX_MESSAGES_PER_WINDOW;
        } catch (Exception e) {
            // Redis 장애 시 Fail-Open: 채팅 전면 중단보다 도배방지 일시 완화가 낫다는 정책
            log.warn("ChatRateLimiter Redis 호출 실패, Fail-Open 처리", e);
            return true;
        }
    }
}
