package com.zerorisk.project.domain.competition.service;

import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import com.zerorisk.project.domain.competition.entity.FailedRecalculation;
import com.zerorisk.project.domain.competition.exception.CompetitionErrorCode;
import com.zerorisk.project.domain.competition.exception.CompetitionException;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.repository.FailedRecalculationRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedRecalculationService {

    private final FailedRecalculationRepository failedRecalculationRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final CompetitionAssetService competitionAssetService;
    private final RecalculationMetrics recalculationMetrics;
    private final AdminActionLogger adminActionLogger;

    @Transactional
    public void saveFailure(Long participantId, String reason) {
        FailedRecalculation failure = FailedRecalculation.builder()
                .participantId(participantId)
                .failureReason(reason)
                .build();
        failedRecalculationRepository.save(failure);
        recalculationMetrics.recordDlqSaved();
    }

    @Transactional(readOnly = true)
    public Page<FailedRecalculation> getFailures(boolean resolved, Pageable pageable) {
        return failedRecalculationRepository.findByResolved(resolved, pageable);
    }

    // 관리자가 수동으로 재처리를 트리거하는 경로. recalculate()를 다시 호출하므로 @Retryable도 그대로 다시 탄다.
    // 스케줄러(자동 재시도) 전용 - 관리자 수동 재처리(retryResolve)와 달리 특정 관리자의 행위가
    // 아니므로 감사 로그를 남기지 않고, 실패해도 예외를 던지지 않아 나머지 건 처리를 막지 않는다.
    @Transactional
    public void retryAutomatically(Long failedRecalculationId) {
        FailedRecalculation failure = failedRecalculationRepository.findById(failedRecalculationId)
                .orElse(null);
        if (failure == null || failure.isResolved()) {
            return;
        }

        CompetitionParticipant participant = competitionParticipantRepository.findById(failure.getParticipantId())
                .orElse(null);
        if (participant == null) {
            return;
        }

        try {
            competitionAssetService.recalculate(participant);
            failure.markResolved();
            recalculationMetrics.recordDlqResolved();
            log.info("재평가 실패 건 자동 재시도 성공 - failedRecalculationId: {}", failedRecalculationId);
        } catch (Exception e) {
            failure.incrementRetryCount();
            log.warn("재평가 실패 건 자동 재시도 실패 - failedRecalculationId: {}, reason: {}",
                    failedRecalculationId, e.getMessage());
        }
    }

    @Transactional
    public void retryResolve(Long failedRecalculationId, Long adminId) {
        FailedRecalculation failure = failedRecalculationRepository.findById(failedRecalculationId)
                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.FAILED_RECALCULATION_NOT_FOUND));

        if (failure.isResolved()) {
            return; // 이미 처리됨 - 중복 재처리 방지 (상태 변경이 없으니 감사 로그도 남기지 않음)
        }

        CompetitionParticipant participant = competitionParticipantRepository.findById(failure.getParticipantId())
                .orElseThrow(() -> new CompetitionException(CompetitionErrorCode.NOT_FOUND));

        try {
            competitionAssetService.recalculate(participant);
            failure.markResolved();
            recalculationMetrics.recordDlqResolved();
            adminActionLogger.log(adminId, "RETRY", "FAILED_RECALCULATION", failedRecalculationId,
                    String.format("재평가 실패 건 #%d 수동 재처리 완료 (참가자ID: %d)",
                            failedRecalculationId, participant.getId()));
        } catch (Exception e) {
            failure.incrementRetryCount();
            log.warn("재평가 실패 건 수동 재처리 실패 - failedRecalculationId: {}, reason: {}",
                    failedRecalculationId, e.getMessage());
            throw e;
        }
    }
}
