package com.zerorisk.project.global.security.ratelimit;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 최근 일정 기간(window) 동안 발생한 이벤트 수를 세는 슬라이딩 윈도우 카운터.
 * 로그인 실패, 회원가입 시도처럼 "최근 N분 내 M회"를 판단해야 하는 경우에 사용한다.
 */
@Component
@RequiredArgsConstructor
public class SlidingWindowCounter {

    private final RedisTemplate<String, String> redisTemplate;

    public void record(String key, Duration window) {
        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID();

        redisTemplate.opsForZSet().add(key, member, now);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - window.toMillis());
        redisTemplate.expire(key, window);
    }

    public long count(String key, Duration window) {
        long windowStart = System.currentTimeMillis() - window.toMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        Long count = redisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }
}
