package com.zerorisk.project.benchmark;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

// 측정 전용 Job/Step 구성. Writer는 카운트만 하는 no-op이라 읽기(페이징) 성능만 순수하게 측정한다.
// spring.batch.job.enabled=false라 앱 기동 시 자동 실행되지 않고, BenchmarkRunner가
// JobLauncher로 직접 트리거할 때만 동작함.
@Configuration
public class BenchmarkBatchConfig {

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public JpaPagingItemReader<BenchmarkLog> offsetReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<BenchmarkLog>()
                .name("offsetReader")
                .entityManagerFactory(emf)
                .queryString("SELECT b FROM BenchmarkLog b ORDER BY b.id")
                .pageSize(CHUNK_SIZE)
                .saveState(false) // 재시작 지원 불필요 - 반복 측정 시 상태 충돌 방지
                .build();
    }

    @Bean
    public KeysetItemReader keysetReader(EntityManager entityManager) {
        return new KeysetItemReader(entityManager);
    }

    @Bean
    public Step offsetStep(JobRepository jobRepository, PlatformTransactionManager tm,
                            JpaPagingItemReader<BenchmarkLog> offsetReader) {
        return new StepBuilder("offsetStep", jobRepository)
                .<BenchmarkLog, BenchmarkLog>chunk(CHUNK_SIZE, tm)
                .reader(offsetReader)
                .writer(items -> { }) // no-op, 읽기 성능만 측정
                .build();
    }

    @Bean
    public Step keysetStep(JobRepository jobRepository, PlatformTransactionManager tm,
                            KeysetItemReader keysetReader) {
        return new StepBuilder("keysetStep", jobRepository)
                .<BenchmarkLog, BenchmarkLog>chunk(CHUNK_SIZE, tm)
                .reader(keysetReader)
                .writer(items -> { })
                .build();
    }

    @Bean
    public Job offsetJob(JobRepository jobRepository, Step offsetStep) {
        return new JobBuilder("offsetJob", jobRepository)
                .start(offsetStep)
                .build();
    }

    @Bean
    public Job keysetJob(JobRepository jobRepository, Step keysetStep) {
        return new JobBuilder("keysetJob", jobRepository)
                .start(keysetStep)
                .build();
    }
}
