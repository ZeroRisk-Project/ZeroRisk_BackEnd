package com.zerorisk.project.domain.competition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 대회 상금 지급 전 참가자 자산 재평가(CompetitionAssetService.recalculate)가
// 재시도(@Retryable) 끝까지 실패했을 때 격리해서 보관하는 DLQ 성격의 테이블.
@Entity
@Table(name = "FAILED_RECALCULATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedRecalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failed_recalc_seq")
    @SequenceGenerator(name = "failed_recalc_seq", sequenceName = "FAILED_RECALCULATIONS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARTICIPANT_ID", nullable = false)
    private Long participantId;

    @Column(name = "FAILURE_REASON", length = 1000)
    private String failureReason;

    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount;

    @Column(name = "RESOLVED", nullable = false)
    private boolean resolved;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 낙관적 락 - 관리자 재처리 API와 배치 재시도가 동시에 같은 건을 건드릴 가능성 대비
    @Version
    private Long version;

    @Builder
    private FailedRecalculation(Long participantId, String failureReason) {
        this.participantId = participantId;
        this.failureReason = failureReason;
        this.retryCount = 0;
        this.resolved = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markResolved() {
        this.resolved = true;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
