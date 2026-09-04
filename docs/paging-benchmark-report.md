# LIMIT-OFFSET vs 키셋 페이징 벤치마크 리포트

**대상 테이블**: BENCHMARK_LOGS (USER_ACTIVITY_LOGS와 동일 구조로 완전 격리)
**환경**: 운영 EC2 Oracle, 같은 zerorisk 계정 안에 별도 테이블로 격리 (새 스키마 아님)
**도구**: Spring Batch · Oracle 19c · JDK 17
**기간**: 2026-09-03 ~ 09-04
**원본 데이터**: [`benchmark-results.csv`](./benchmark-results.csv), [`gc-offset.log`](./gc-offset.log), [`gc-keyset.log`](./gc-keyset.log)
**실행 코드**: `src/main/java/com/zerorisk/project/benchmark/`

---

## 요약

- 100만 건 기준, 10만 건 대비 소요시간 증가배율: **LIMIT-OFFSET 24.34배 vs 키셋 10.04배**
- 키셋은 데이터 10배 증가에 시간도 거의 정확히 10배 — 이론적인 선형(O(n))과 거의 일치
- GC 로그 실험 도중 벤치마크 코드 자체의 버그 1건 발견·수정 (`KeysetItemReader` 영속성 컨텍스트 누수)
- 두 방식 모두 100만 건까지 Full GC 발생 0건

---

## 0. 방법론 & 안전장치

실제 서비스 DB(EC2)를 대상으로 측정하되, 디스크 장애를 반복하지 않도록 운영 데이터와 완전히 분리한 상태로 진행했다.

- **테이블 격리** — 새 Oracle 스키마 대신, 같은 `zerorisk` 계정 안에 `USER_ACTIVITY_LOGS`와 무관한 `BENCHMARK_LOGS` 테이블을 별도 생성. 컬럼 구조는 동일하게 맞춰 공정 비교하고, 실제 활동 로그는 한 줄도 건드리지 않음.
- **스케줄러 차단** — 벤치마크를 위해 앱 전체를 부팅해야 했는데, 대회 상태 전환·예약 주문 체결·목표가 알림 등 8개의 `@Scheduled` 스케줄러가 같이 깨어나는 걸 막기 위해 `app.scheduling.enabled=false` 조건을 전 스케줄러에 적용하고 전용 `benchmark` 프로필로만 실행.
- **실행 안전장치** — `--benchmark.mode`를 필수 인자로 강제(기본값으로 대량 시딩이 실행되는 걸 방지), 첫 실행은 데이터 0건 상태의 스모크 테스트로 스키마 생성만 확인 후 진행.
- **측정 도구** — LIMIT-OFFSET은 Spring Batch 내장 `JpaPagingItemReader`, 키셋은 `WHERE id > :lastId ORDER BY id` 방식의 커스텀 Reader. 청크 크기 1,000건, 각 조합 3회 반복 측정 후 평균.
- **실행 후 정리** — `--benchmark.mode=drop`으로 `BENCHMARK_LOGS`/시퀀스와 Spring Batch 메타데이터 테이블(`BATCH_JOB_INSTANCE` 등 6개 테이블 + 시퀀스 3개)까지 전부 삭제 완료.

### 실행 커맨드 참고

```bash
# 스모크 테스트 (스키마만 생성, 데이터 0건)
./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=smoke"

# 시딩 + 페이징 벤치마크 (offset/keyset 둘 다)
./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=run --benchmark.volume=1000000"

# 시딩만 (인덱스 실험 등 데이터만 필요할 때)
./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=seed --benchmark.volume=1000000"

# 한쪽 reader만 (GC 로그 분리 등)
java -Xlog:gc*:file=gc-keyset.log:time,uptime:filecount=5,filesize=10M \
     -jar build/libs/zerorisk.jar \
     --spring.profiles.active=local,benchmark --benchmark.mode=run \
     --benchmark.volume=1000000 --benchmark.reader=keyset

# 정리 (테이블/시퀀스/Batch 메타데이터 전체 삭제)
./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=drop"
```

---

## 1. 페이징 성능 비교

| 데이터 건수 | LIMIT-OFFSET 평균 | 키셋 평균 | 차이 | OFFSET 증가배율(10만 대비) | 키셋 증가배율(10만 대비) |
|---|---|---|---|---|---|
| 100,000 | 16,988 ms | 14,699 ms | 1.16× | 1.00× | 1.00× |
| 500,000 | 140,865 ms | 75,242 ms | 1.87× | 8.29× | 5.12× |
| 1,000,000 | 413,551 ms | 147,624 ms | 2.80× | 24.34× | 10.04× |

100만 건은 이후 독립 재실행(GC 로그 실험용)에서도 offset 399,859ms, keyset 130,753~139,891ms 범위로 재현됨.

**왜 이렇게 되는가**: 키셋은 항상 "마지막으로 본 ID보다 큰 것 중 다음 1,000개"만 찾으면 되므로 페이지 조회 비용이 데이터량과 무관하게 일정 — 전체 시간이 데이터량에 정비례한다. LIMIT-OFFSET은 뒤로 갈수록 "건너뛰어야 할 앞부분"이 계속 늘어나, 페이지가 깊어질수록 한 페이지 조회 비용 자체가 커진다 — 그래서 데이터량보다 가파르게(초선형) 늘어난다.

---

## 2. 인덱스 실험 — 준비 완료 · 미실행

필터 조건이 있는 조회(특정 활동 유형만 조회)에서 인덱스가 실질적으로 얼마나 도움이 되는지 확인하려던 실험. GC 로그 실험을 우선하기로 하면서 **데이터 준비까지만** 진행했다.

- **완료된 것**: `BENCHMARK_LOGS` 100만 건을 `ACTION_TYPE` 10종(LOGIN / SIGNUP / ORDER_BUY / ORDER_SELL / FOLLOW / UNFOLLOW / POST_CREATE / COMMENT_CREATE / CHARGE / WITHDRAW)에 정확히 100,000건씩 균등 분배해 재시딩 완료 (`--benchmark.mode=seed`로 확인).
- **남은 것**: 인덱스 없는 상태에서 `ACTION_TYPE = 'LOGIN' AND ID > 500000` 조건의 `EXPLAIN PLAN` 확인 → `CREATE INDEX` 생성 → 같은 쿼리 재실행 후 `TABLE ACCESS FULL`이 `INDEX RANGE SCAN`으로 바뀌는지, Cost가 얼마나 줄어드는지 캡처.
- **참고**: 이번 리포트를 마무리하며 `BENCHMARK_LOGS`를 완전히 삭제(`--benchmark.mode=drop`)했으므로, 이 실험을 이어가려면 `--benchmark.mode=seed --benchmark.volume=1000000`로 데이터를 다시 준비해야 한다.

---

## 3. JVM GC 로그 실험

"LIMIT-OFFSET은 읽었다 버리는 객체가 많아서 JVM Old Gen에 더 부담을 줄 것"이라는 가설로 시작했다. 결과는 정반대로 나왔고, 원인을 추적하는 과정에서 벤치마크 코드 자체의 버그를 발견했다.

### 실행 방법

두 방식을 같은 프로세스에서 같이 측정하면 GC 로그가 섞여 원인을 구분할 수 없다. `--benchmark.reader=offset|keyset` 옵션을 새로 추가해 한 번에 한 방식만 실행하도록 분리했고, Gradle을 거치지 않고 빌드된 jar를 직접 실행해 JVM 옵션을 확실하게 제어했다.

### 타임라인

1. **1차 측정** — LIMIT-OFFSET과 키셋을 각각 독립된 JVM에서 GC 로그와 함께 100만 건 재측정.
2. **예상과 반대의 결과** — Old Gen 최대 사용량이 OFFSET 79M보다 **키셋 119M이 더 높게** 나옴. 가설과 정반대.
3. **원인 발견 & 수정** — `KeysetItemReader`가 청크마다 `EntityManager`를 재사용하면서 영속성 컨텍스트(1차 캐시)를 한 번도 비우지 않고 있었음. 다음 청크를 읽기 전 `entityManager.clear()`를 추가.
4. **재측정 & 재해석** — 수정 후에도 키셋의 Old Gen(99M)이 offset(79M)보다 여전히 소폭 높음 — 근본 원인을 다시 짚어봄.

### 발견한 버그

Spring Batch 내장 `JpaPagingItemReader`(offset)는 페이지마다 영속성 컨텍스트를 자동으로 비우는데, 직접 만든 `KeysetItemReader`는 그 처리가 빠져 있었다. 같은 `EntityManager`를 계속 재사용하다 보니, 청크를 읽을 때마다 이미 읽은 엔티티가 1차 캐시에 계속 쌓였다 — 100만 건 × 3회 반복이면 최대 300만 개까지 붙잡혀 있을 수 있는 구조였다.

```java
if (currentIndex >= currentChunk.size()) {
    entityManager.clear(); // 다음 청크 로드 전 영속성 컨텍스트를 비움 (추가한 부분)
    currentChunk = entityManager.createQuery(
            "SELECT b FROM BenchmarkLog b WHERE b.id > :lastId ORDER BY b.id",
            BenchmarkLog.class)
        .setParameter("lastId", lastId)
        .setMaxResults(CHUNK_SIZE)
        .getResultList();
    // ...
}
```

### 수정 전후 비교

| | Old Gen 최대 | GC 횟수 | 총 pause | Full GC |
|---|---|---|---|---|
| OFFSET | 79M | 206 | 767.8ms | 0 |
| 키셋(버그) | 119M | 210 | 824.5ms | 0 |
| 키셋(수정) | 99M | 232 | 850.0ms | 0 |

### 재해석 — 결론

버그를 고친 뒤에도 키셋의 Old Gen(99M)과 GC 횟수(232회)가 offset(79M / 206회)보다 여전히 소폭 높다. 억지로 가설에 맞추지 않고 있는 그대로 해석하면:

**LIMIT-OFFSET의 성능 저하는 JVM 힙 압박 때문이 아니라, DB 서버 측 스캔 비용 때문일 가능성이 크다.** Oracle이 OFFSET만큼 행을 건너뛸 때, 건너뛴 행의 데이터는 JDBC로 애플리케이션까지 전송되지 않고 DB 엔진 내부에서만 스캔되고 버려진다 — 애플리케이션 힙에는 애초에 부담이 갈 여지가 없는 구조다. 반대로 키셋은 매 청크마다 새 쿼리 객체와 파라미터 바인딩을 반복 생성하는데, 이게 성숙한 `JpaPagingItemReader`보다 청크당 가비지를 조금 더 만든다.

두 방식 모두 100만 건까지 **Full GC는 0건** — 이 규모에서는 JVM 메모리 자체가 위험 수준은 아니라는 뜻이다. 1절의 24배 격차는 GC가 아니라 **DB 스캔 비용**에서 나온다는 게 이번 실험이 실제로 보여준 것이다.

---

## 4. 결론 & 권장사항

- **관리자 활동 로그 조회를 키셋 페이징으로 전환** — 데이터가 계속 쌓이는 구조상, 100만 건 시점에 이미 2.8배 차이가 나고 격차는 계속 벌어진다. 뒷페이지 조회가 잦은 화면일수록 효과가 크다.
- **2절 인덱스 실험 이어서 진행** — `--benchmark.mode=seed`로 데이터부터 다시 준비하면 `EXPLAIN PLAN` 전/후 비교만 남는다.
- **실제 키셋 Reader를 만들 때는 이번에 발견한 버그를 그대로 재현하지 말 것** — `EntityManager`를 청크 단위로 재사용한다면 반드시 주기적으로 `clear()`할 것.
- **`BENCHMARK_LOGS`와 Spring Batch 메타데이터 테이블은 이 리포트 작성 시점에 전부 삭제 완료** — 재실행 시 `smoke` 모드로 스키마부터 다시 확인할 것.
