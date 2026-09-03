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
