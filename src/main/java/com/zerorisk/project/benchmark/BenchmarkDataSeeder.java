package com.zerorisk.project.benchmark;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// JDBC 배치 삽입으로 BENCHMARK_LOGS를 채운다. USER_ACTIVITY_LOGS는 절대 건드리지 않음.
// DETAIL 필드에 "벤치마크 테스트 데이터"를 항상 포함시켜, 혹시 다른 목적으로 조회하더라도
// 한눈에 벤치마크용 데이터임을 알 수 있게 한다 (BENCHMARK_LOGS 자체가 이미 별도 테이블이라
// 실제 활동 로그와 섞일 일은 없지만, 식별 문구는 안전장치로 유지).
@Slf4j
@Component
@RequiredArgsConstructor
public class BenchmarkDataSeeder {

    private static final String INSERT_SQL =
            "INSERT INTO BENCHMARK_LOGS (ID, USER_ID, ACTION_TYPE, DETAIL, IP_ADDRESS, CREATED_AT) "
                    + "VALUES (BENCHMARK_LOGS_SEQ.NEXTVAL, ?, ?, ?, ?, ?)";

    // 인덱스 실험(특정 action_type만 조회하는 시나리오)을 위해 10종류로 균등 분배.
    // i % ACTION_TYPES.length라 totalCount가 10의 배수면 각 타입이 정확히 totalCount/10건씩 나온다.
    private static final String[] ACTION_TYPES = {
            "LOGIN", "SIGNUP", "ORDER_BUY", "ORDER_SELL", "FOLLOW",
            "UNFOLLOW", "POST_CREATE", "COMMENT_CREATE", "CHARGE", "WITHDRAW"
    };

    private final JdbcTemplate jdbcTemplate;

    // 기존 데이터를 비우고 정확히 totalCount건으로 다시 채운다 (데이터 규모별 측정 시점의
    // 총 건수를 정확히 맞추기 위해 - 누적시키면 이전 실행분이 섞여 건수가 불명확해짐).
    public void reseed(int totalCount, int batchSize) {
        log.info("[BENCHMARK] BENCHMARK_LOGS 초기화 후 {}건 시딩 시작", totalCount);
        truncate();

        List<Object[]> batch = new ArrayList<>(batchSize);
        LocalDateTime base = LocalDateTime.now();

        for (int i = 0; i < totalCount; i++) {
            batch.add(new Object[]{
                    (long) (i % 1000) + 1, // 가짜 userId 1~1000 순환
                    ACTION_TYPES[i % ACTION_TYPES.length],
                    "벤치마크 테스트 데이터 " + i,
                    "127.0.0.1",
                    Timestamp.valueOf(base.minusSeconds(i))
            });

            if (batch.size() == batchSize) {
                jdbcTemplate.batchUpdate(INSERT_SQL, batch);
                batch.clear();

                if ((i + 1) % 100_000 == 0) {
                    log.info("[BENCHMARK] 시딩 진행: {}/{}", i + 1, totalCount);
                }
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SQL, batch);
        }

        log.info("[BENCHMARK] 시딩 완료: {}건", count());
    }

    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM BENCHMARK_LOGS", Long.class);
        return result == null ? 0L : result;
    }

    public void truncate() {
        jdbcTemplate.execute("TRUNCATE TABLE BENCHMARK_LOGS");
    }
}
