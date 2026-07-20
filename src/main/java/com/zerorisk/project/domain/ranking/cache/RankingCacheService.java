package com.zerorisk.project.domain.ranking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.domain.ranking.dto.RankingResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 주의: 이 클래스는 "정본(Oracle RANK() 계산 결과)"을 그대로 옮겨 담는 캐시 레이어일 뿐,
// 순위/수익률을 여기서 다시 계산하지 않음. Scheduler가 주기적으로 refresh()를 호출해서 갱신함.
@Component
public class RankingCacheService {

    private static final String ZSET_KEY = "ranking:global:zset";
    private static final String META_KEY_PREFIX = "ranking:global:meta:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RankingCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // Scheduler가 호출: 기존 캐시를 전부 지우고 최신 계산 결과로 통째로 교체
    public void refresh(List<RankingResponse> rankings) {
        redisTemplate.delete(ZSET_KEY);

        for (RankingResponse ranking : rankings) {
            String userId = String.valueOf(ranking.userId());

            redisTemplate.opsForZSet().add(ZSET_KEY, userId, ranking.returnRate().doubleValue());
            redisTemplate.opsForValue().set(META_KEY_PREFIX + userId, toJson(ranking));
        }
    }

    public List<RankingResponse> getTopRankings(int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(ZSET_KEY,
                start, end);

        if (tuples == null) {
            return List.of();
        }

        List<RankingResponse> result = new ArrayList<>();
        int rank = (int) start + 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            RankingResponse cached = fromJson(redisTemplate.opsForValue().get(META_KEY_PREFIX + tuple.getValue()));

            if (cached != null) {
                result.add(new RankingResponse(rank, cached.userId(), cached.nickname(), cached.userLevel(),
                        cached.returnRate()));
            }

            rank++;
        }

        return result;
    }

    public RankingResponse getMyRanking(Long userId) {
        String key = String.valueOf(userId);

        Long reverseRank = redisTemplate.opsForZSet().reverseRank(ZSET_KEY, key);
        RankingResponse cached = fromJson(redisTemplate.opsForValue().get(META_KEY_PREFIX + key));

        if (reverseRank == null || cached == null) {
            return null;
        }

        return new RankingResponse(reverseRank.intValue() + 1, cached.userId(), cached.nickname(), cached.userLevel(),
                cached.returnRate());
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