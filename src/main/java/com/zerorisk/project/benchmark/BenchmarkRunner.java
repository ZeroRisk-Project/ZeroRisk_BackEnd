package com.zerorisk.project.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// benchmark 프로필로 앱을 띄웠을 때만 동작 (spring.batch.job.enabled=false라 다른 프로필에선
// Job이 자동 실행되지 않고, 이 러너도 @Profile("benchmark")라 그 프로필에서만 빈으로 등록됨).
//
// 반드시 local과 함께 활성화할 것 (local,benchmark) - benchmark 프로필만 단독으로 주면
// application-local.properties(DB_HOST/Redis 접속 정보 등)가 로드되지 않아 접속 자체가 깨짐.
//
// 사용법 (한 번에 한 데이터 규모씩 - 100만 건까지 한 프로세스에서 다 몰아서 돌리면
// 중간에 문제 생겼을 때 처음부터 다시 해야 해서, 볼륨 단위로 나눠서 실행하도록 설계함):
//   ./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=run --benchmark.volume=100000 --benchmark.repeat=3"
//   (--benchmark.reader=offset|keyset|both(기본값) 로 한쪽만 골라 실행 가능 - GC 로그처럼 두 방식을 같은 프로세스에서 섞으면 안 될 때 사용)
//   ./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=seed --benchmark.volume=1000000" (시딩만, 페이징 벤치마크 실행 안 함 - 인덱스 실험 등 데이터만 필요할 때)
//   ./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=cleanup"   (TRUNCATE만)
//   ./gradlew bootRun --args="--spring.profiles.active=local,benchmark --benchmark.mode=drop"       (테이블/시퀀스 완전 삭제)
@Slf4j
@Component
@Profile("benchmark")
@RequiredArgsConstructor
public class BenchmarkRunner implements CommandLineRunner {

    private static final Path RESULT_CSV = Path.of("benchmark-results.csv");

    private final JobLauncher jobLauncher;
    private final Job offsetJob;
    private final Job keysetJob;
    private final BenchmarkDataSeeder seeder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 기본값을 "run"(시딩+벤치마크 실행)으로 두면, mode를 깜빡하고 안 준 채 그냥 띄우기만
        // 해도 곧바로 대량 시딩이 시작되는 위험한 구조라 일부러 기본값을 없앰(mode 필수).
        String mode = argValue(args, "benchmark.mode", null);
        if (mode == null) {
            throw new IllegalArgumentException(
                    "--benchmark.mode를 반드시 지정해야 합니다 (smoke | seed | run | cleanup | drop)");
        }

        switch (mode) {
            case "smoke" -> checkSchema(); // 스키마(엔티티/배치 메타 테이블)만 생성되는지 확인 - 데이터 0건, 아무 것도 실행 안 함
            case "seed" -> seedOnly(args); // 시딩만 - 페이징 벤치마크(offsetJob/keysetJob)는 실행하지 않음
            case "cleanup" -> {
                log.info("[BENCHMARK] BENCHMARK_LOGS TRUNCATE");
                seeder.truncate();
            }
            case "drop" -> dropAll();
            case "run" -> runBenchmark(args);
            default -> throw new IllegalArgumentException("알 수 없는 benchmark.mode: " + mode);
        }
    }

    // 벤치마크가 남긴 흔적을 전부 지운다: 데이터 테이블 + Spring Batch 메타데이터 테이블/시퀀스까지.
    // CASCADE CONSTRAINTS로 FK 순서를 신경 안 써도 되게 하고, 하나가 없어도(이미 지워졌거나
    // 애초에 없었던 경우) 나머지는 계속 진행하도록 각각 개별 try-catch로 감쌌다.
    private void dropAll() {
        log.info("[BENCHMARK] 벤치마크 테이블/시퀀스/Spring Batch 메타데이터 전체 삭제 시작");

        dropQuietly("DROP TABLE BENCHMARK_LOGS CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP SEQUENCE BENCHMARK_LOGS_SEQ");

        dropQuietly("DROP TABLE BATCH_STEP_EXECUTION_CONTEXT CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP TABLE BATCH_JOB_EXECUTION_CONTEXT CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP TABLE BATCH_STEP_EXECUTION CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP TABLE BATCH_JOB_EXECUTION_PARAMS CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP TABLE BATCH_JOB_EXECUTION CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP TABLE BATCH_JOB_INSTANCE CASCADE CONSTRAINTS PURGE");
        dropQuietly("DROP SEQUENCE BATCH_STEP_EXECUTION_SEQ");
        dropQuietly("DROP SEQUENCE BATCH_JOB_EXECUTION_SEQ");
        dropQuietly("DROP SEQUENCE BATCH_JOB_SEQ");

        log.info("[BENCHMARK] 정리 완료");
    }

    private void dropQuietly(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("[BENCHMARK]   OK: {}", sql);
        } catch (Exception e) {
            log.warn("[BENCHMARK]   건너뜀 (이미 없거나 실패): {} - {}", sql, e.getMessage());
        }
    }

    private void seedOnly(String... args) {
        int volume = Integer.parseInt(argValue(args, "benchmark.volume", "100000"));
        int batchSize = 1000;

        log.info("[BENCHMARK] seed 모드 - {}건 시딩만 진행합니다 (페이징 벤치마크는 실행하지 않음)", volume);
        seeder.reseed(volume, batchSize);

        log.info("[BENCHMARK] === ACTION_TYPE별 분포 확인 ===");
        jdbcTemplate.queryForList(
                "SELECT action_type, COUNT(*) AS cnt FROM BENCHMARK_LOGS GROUP BY action_type ORDER BY action_type"
        ).forEach(row -> log.info("[BENCHMARK]   {}: {}건", row.get("ACTION_TYPE"), row.get("CNT")));
    }

    private void runBenchmark(String... args) throws Exception {
        int volume = Integer.parseInt(argValue(args, "benchmark.volume", "100000"));
        int repeat = Integer.parseInt(argValue(args, "benchmark.repeat", "3"));
        // GC 로그처럼 "이번 프로세스는 한 방식만" 필요한 실험을 위한 옵션. 기본값 both는 기존 동작 그대로.
        String reader = argValue(args, "benchmark.reader", "both");
        int batchSize = 1000;

        log.info("[BENCHMARK] 데이터 {}건, reader={}, 각 방식 {}회 반복 측정 시작", volume, reader, repeat);
        seeder.reseed(volume, batchSize);

        Long offsetAvg = reader.equals("offset") || reader.equals("both")
                ? measure(offsetJob, "offset", volume, repeat) : null;
        Long keysetAvg = reader.equals("keyset") || reader.equals("both")
                ? measure(keysetJob, "keyset", volume, repeat) : null;

        if (offsetAvg != null && keysetAvg != null) {
            log.info("[BENCHMARK] 결과 - 데이터 {}건: LIMIT-OFFSET 평균 {}ms, 키셋 평균 {}ms (차이 {}배)",
                    volume, offsetAvg, keysetAvg,
                    keysetAvg == 0 ? "N/A" : String.format("%.2f", (double) offsetAvg / keysetAvg));
        }

        if (offsetAvg != null) {
            appendResult("offset", volume, offsetAvg);
        }
        if (keysetAvg != null) {
            appendResult("keyset", volume, keysetAvg);
        }
        printDegradationTable();
    }

    private long measure(Job job, String label, int volume, int repeat) throws Exception {
        List<Long> durations = new ArrayList<>();

        for (int i = 0; i < repeat; i++) {
            var jobParameters = new JobParametersBuilder()
                    .addString("volume", String.valueOf(volume))
                    .addString("reader", label)
                    .addLong("run", System.nanoTime()) // 매 실행마다 유일해야 JobRepository가 재실행을 허용함
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, jobParameters);

            long stepDurationMs = execution.getStepExecutions().stream()
                    .mapToLong(this::stepDurationMs)
                    .sum();

            durations.add(stepDurationMs);
            log.info("[BENCHMARK] {} {}건 - {}회차: {}ms", label, volume, i + 1, stepDurationMs);
        }

        return (long) durations.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long stepDurationMs(StepExecution stepExecution) {
        if (stepExecution.getStartTime() == null || stepExecution.getEndTime() == null) {
            return 0L;
        }
        return java.time.Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis();
    }

    // 이번 실행 결과를 CSV에 누적 기록 - 10만/50만/100만을 서로 다른 시점(프로세스)에 나눠 돌려도
    // 마지막에 파일 하나로 모아서 표를 만들 수 있게 하기 위함.
    private void appendResult(String readerType, int volume, long avgMs) {
        boolean isNewFile = !Files.exists(RESULT_CSV);
        try (FileWriter writer = new FileWriter(RESULT_CSV.toFile(), true)) {
            if (isNewFile) {
                writer.write("timestamp,readerType,dataVolume,avgExecutionTimeMs\n");
            }
            writer.write(String.format("%s,%s,%d,%d%n",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    readerType, volume, avgMs));
        } catch (IOException e) {
            log.warn("[BENCHMARK] 결과 CSV 기록 실패 - 로그의 평균값을 수동으로 기록해두세요", e);
        }
    }

    // 지금까지 CSV에 쌓인 결과 중, readerType별 최소 데이터 규모를 기준(1.0x)으로 증가배율을 계산해 출력.
    private void printDegradationTable() {
        if (!Files.exists(RESULT_CSV)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(RESULT_CSV);
            record Row(String readerType, int volume, long avgMs) {
            }
            List<Row> rows = lines.stream()
                    .skip(1)
                    .map(line -> line.split(","))
                    .map(cols -> new Row(cols[1], Integer.parseInt(cols[2]), Long.parseLong(cols[3])))
                    .toList();

            for (String readerType : List.of("offset", "keyset")) {
                List<Row> readerRows = rows.stream().filter(r -> r.readerType().equals(readerType)).toList();
                if (readerRows.isEmpty()) {
                    continue;
                }
                long baseline = readerRows.stream().mapToLong(Row::avgMs).min().orElse(1);
                if (baseline == 0) {
                    baseline = 1;
                }
                log.info("[BENCHMARK] === {} 증가배율 (최소 데이터 규모 대비) ===", readerType);
                for (Row row : readerRows) {
                    double ratio = (double) row.avgMs() / baseline;
                    log.info("[BENCHMARK]   {}건: {}ms ({}x)", row.volume(), row.avgMs(),
                            String.format("%.2f", ratio));
                }
            }
        } catch (IOException e) {
            log.warn("[BENCHMARK] 결과 CSV 읽기 실패", e);
        }
    }

    // 로그를 눈으로 훑어서 유추하지 않고, USER_TABLES를 직접 조회해서 확실하게 확인한다.
    private void checkSchema() {
        List<String> expectedTables = List.of(
                "BENCHMARK_LOGS", "BATCH_JOB_INSTANCE", "BATCH_JOB_EXECUTION",
                "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION_PARAMS");

        for (String table : expectedTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?", Integer.class, table);
            boolean exists = count != null && count > 0;
            log.info("[BENCHMARK] [SCHEMA_CHECK] {} : {}", table, exists ? "존재함" : "!!! 없음 !!!");
        }
        log.info("[BENCHMARK] smoke 모드 완료 - 시딩/벤치마크는 실행하지 않았습니다");
    }

    private String argValue(String[] args, String key, String defaultValue) {
        String prefix = "--" + key + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return defaultValue;
    }
}
