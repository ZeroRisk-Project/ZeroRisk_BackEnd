package com.zerorisk.project.domain.ranking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.domain.ranking.dto.RankingPeriod;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 주의: 이 클래스는 "정본(Oracle RANK() 계산 결과)"을 그대로 옮겨 담는 캐시 레이어일 뿐,
// 순위/수익률을 여기서 다시 계산하지 않음. Scheduler가 주기적으로 refresh()를 호출해서 갱신함.
// 기간(일간/주간/월간/전체)마다 랭킹이 다르므로 zset/meta 키를 기간별로 완전히 분리해서 보관함.
@Component
public class RankingCacheService {

    private static final String ZSET_KEY_PREFIX = "ranking:global:zset:";
    private static final String META_KEY_PREFIX = "ranking:global:meta:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RankingCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // Scheduler가 호출: 해당 기간의 기존 캐시를 전부 지우고 최신 계산 결과로 통째로 교체
    public void refresh(RankingPeriod period, List<RankingResponse> rankings) {
        String zsetKey = zsetKey(period);
        redisTemplate.delete(zsetKey);

        for (RankingResponse ranking : rankings) {
            String userId = String.valueOf(ranking.userId());

            redisTemplate.opsForZSet().add(zsetKey, userId, ranking.returnRate().doubleValue());
            redisTemplate.opsForValue().set(metaKey(period, userId), toJson(ranking));
        }
    }

    public List<RankingResponse> getTopRankings(RankingPeriod period, int page, int size) {
        String zsetKey = zsetKey(period);
        long start = (long) page * size;
        long end = start + size - 1;

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(zsetKey,
                start, end);

        if (tuples == null) {
            return List.of();
        }

        List<RankingResponse> result = new ArrayList<>();
        int rank = (int) start + 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            RankingResponse cached = fromJson(redisTemplate.opsForValue().get(metaKey(period, tuple.getValue())));

            if (cached != null) {
                result.add(new RankingResponse(rank, cached.userId(), cached.nickname(), cached.userLevel(),
                        cached.returnRate(), cached.baseDate()));
            }

            rank++;
        }

        return result;
    }

    public RankingResponse getMyRanking(RankingPeriod period, Long userId) {
        String key = String.valueOf(userId);

        Long reverseRank = redisTemplate.opsForZSet().reverseRank(zsetKey(period), key);
        RankingResponse cached = fromJson(redisTemplate.opsForValue().get(metaKey(period, key)));

        if (reverseRank == null || cached == null) {
            return null;
        }

        return new RankingResponse(reverseRank.intValue() + 1, cached.userId(), cached.nickname(), cached.userLevel(),
                cached.returnRate(), cached.baseDate());
    }

    private String zsetKey(RankingPeriod period) {
        return ZSET_KEY_PREFIX + period.name().toLowerCase();
    }

    private String metaKey(RankingPeriod period, String userId) {
        return META_KEY_PREFIX + period.name().toLowerCase() + ":" + userId;
    }

    private String toJson(RankingResponse ranking) {
        try {
            return objectMapper.writeValueAsString(ranking);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("랭킹 캐시 직렬화 실패", e);
        }
    }

    private RankingResponse fromJson(String json) {
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, RankingResponse.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
