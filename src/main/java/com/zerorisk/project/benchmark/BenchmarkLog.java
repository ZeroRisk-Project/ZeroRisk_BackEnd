package com.zerorisk.project.benchmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// USER_ACTIVITY_LOGS와 완전히 별개인 벤치마크 전용 테이블. 실제 활동 로그와 절대 섞이지 않도록
// 컬럼 구조만 동일하게 맞추고(공정한 비교를 위해) 물리적으로는 다른 테이블/시퀀스를 쓴다.
// 벤치마크가 끝나면 BenchmarkRunner의 cleanup 모드로 이 테이블 자체를 DROP하면 됨.
@Entity
@Table(name = "BENCHMARK_LOGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BenchmarkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benchmark_logs_seq")
    @SequenceGenerator(name = "benchmark_logs_seq", sequenceName = "BENCHMARK_LOGS_SEQ", allocationSize = 50)
    private Long id;

    @Column(name = "USER_ID", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "ACTION_TYPE", nullable = false, length = 30, updatable = false)
    private String actionType;

    @Column(name = "DETAIL", length = 500, updatable = false)
    private String detail;

    @Column(name = "IP_ADDRESS", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
