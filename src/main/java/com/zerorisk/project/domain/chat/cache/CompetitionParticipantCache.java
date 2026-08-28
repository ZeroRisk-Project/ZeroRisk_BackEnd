package com.zerorisk.project.domain.chat.cache;

import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionParticipantCache {

    private static final String KEY_PREFIX = "competition:";
    private static final String KEY_SUFFIX = ":participants";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final CompetitionParticipantRepository competitionParticipantRepository;

    // true = 참가자 맞음 (입장 허용)
    public boolean isParticipant(Long competitionId, Long userId) {
        String key = buildKey(competitionId);

        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(userId));

            if (Boolean.TRUE.equals(isMember)) {
                return true;
            }

            // 캐시에 없다고 해서 바로 "아니오"로 단정하지 않고, 콜드 캐시일 수 있으니 DB로 폴백해서 재확인
            boolean isParticipantInDb = checkFromDb(competitionId, userId);

            if (isParticipantInDb) {
                add(competitionId, userId);
            }

            return isParticipantInDb;
        } catch (Exception e) {
            log.warn("CompetitionParticipantCache Redis 조회 실패, DB로 폴백", e);

            try {
                return checkFromDb(competitionId, userId);
            } catch (Exception dbException) {
                // Fail-Closed: Redis도, DB 폴백도 실패하면 비참가자 유입 방지를 위해 거부
                log.error("CompetitionParticipantCache DB 폴백도 실패, Fail-Closed 처리", dbException);
                return false;
            }
        }
    }

    // 대회 참가 시(POST /competitions/{id}/join) 호출해서 캐시에 반영
    public void add(Long competitionId, Long userId) {
        try {
            String key = buildKey(competitionId);
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            redisTemplate.expire(key, CACHE_TTL);
        } catch (Exception e) {
            log.warn("CompetitionParticipantCache 캐시 추가 실패", e);
        }
    }

    // 강제 퇴장/실격 처리 시 호출해서 캐시에서 즉시 제거
    public void remove(Long competitionId, Long userId) {
        try {
            redisTemplate.opsForSet().remove(buildKey(competitionId), String.valueOf(userId));
        } catch (Exception e) {
            log.warn("CompetitionParticipantCache 캐시 제거 실패", e);
        }
    }

    // 대회 종료 시 호출해서 캐시 전체를 지움
    public void evictAll(Long competitionId) {
        try {
            redisTemplate.delete(buildKey(competitionId));
        } catch (Exception e) {
            log.warn("CompetitionParticipantCache 캐시 전체 삭제 실패", e);
        }
    }

    private boolean checkFromDb(Long competitionId, Long userId) {
        return competitionParticipantRepository.findByCompetitionIdAndUserId(competitionId, userId).isPresent();
    }

    private String buildKey(Long competitionId) {
        return KEY_PREFIX + competitionId + KEY_SUFFIX;
    }
}
