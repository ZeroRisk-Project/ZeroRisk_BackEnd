package com.zerorisk.project.global.websocket.status;

import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import com.zerorisk.project.domain.user.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusChecker {

    private static final String KEY_PREFIX = "chat:userstatus:suspended:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    // true = 정지 상태 (채팅 불가)
    public boolean isSuspended(Long userId) {
        String key = KEY_PREFIX + userId;

        try {
            String cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                return "1".equals(cached);
            }

            boolean suspended = checkFromDb(userId);
            redisTemplate.opsForValue().set(key, suspended ? "1" : "0", CACHE_TTL);

            return suspended;
        } catch (Exception e) {
            // Redis 장애 시 Fail-Open: 정지 여부 판단 실패해도 채팅은 통과시킴
            log.warn("UserStatusChecker Redis 호출 실패, Fail-Open 처리", e);
            return false;
        }
    }

    // 관리자가 유저를 정지/해제 처리할 때 즉시 무효화하기 위한 메서드
    public void evict(Long userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("UserStatusChecker 캐시 무효화 실패", e);
        }
    }

    private boolean checkFromDb(Long userId) {
        return userRepository.findById(userId)
                .map(User::getStatus)
                .map(status -> status == UserStatus.SUSPENDED)
                .orElse(false);
    }
}
