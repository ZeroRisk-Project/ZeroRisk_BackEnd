package com.zerorisk.project.global.security.ratelimit;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 동일 동작의 연속 재시도를 일정 시간 동안 막는 쿨다운 체커.
 * 이메일 인증번호 재발송처럼 "N초 이내 재요청 금지"에 사용한다.
 */
@Component
@RequiredArgsConstructor
public class CooldownChecker {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryAcquire(String key, Duration cooldown) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldown);
        return Boolean.TRUE.equals(acquired);
    }
}
