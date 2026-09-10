package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.FailedRecalculation;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedRecalculationRepository extends JpaRepository<FailedRecalculation, Long> {
    Page<FailedRecalculation> findByResolved(boolean resolved, Pageable pageable);

    boolean existsByParticipantIdAndResolvedFalse(Long participantId);

    // 자동 재시도 스케줄러용 - 너무 많이 재시도해서 계속 실패하는 건(예: 상장폐지 등 영구 실패)까지
    // 매시간 KIS를 두들기지 않도록 재시도 횟수 상한을 둔다.
    List<FailedRecalculation> findByResolvedFalseAndRetryCountLessThan(int retryCount);
}
