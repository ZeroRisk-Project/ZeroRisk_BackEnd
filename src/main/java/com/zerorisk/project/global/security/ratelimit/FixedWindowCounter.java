package com.zerorisk.project.global.security.ratelimit;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 고정된 기간(예: 하루) 동안의 누적 횟수를 세는 카운터.
 * 이메일 인증번호 발송 일일 한도처럼 "하루 최대 N회" 제한에 사용한다.
 */
@Component
@RequiredArgsConstructor
public class FixedWindowCounter {

    private final RedisTemplate<String, String> redisTemplate;

    public long increment(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null ? 0 : count;
    }
}
