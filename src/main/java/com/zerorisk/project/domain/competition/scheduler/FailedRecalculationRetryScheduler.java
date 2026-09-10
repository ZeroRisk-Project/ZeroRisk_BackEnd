package com.zerorisk.project.domain.competition.scheduler;

import com.zerorisk.project.domain.competition.entity.FailedRecalculation;
import com.zerorisk.project.domain.competition.repository.FailedRecalculationRepository;
import com.zerorisk.project.domain.competition.service.FailedRecalculationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// FAILED_RECALCULATIONS(자산 재평가 실패 DLQ)는 지금까지 관리자가 수동으로 재처리(retryResolve)해야만
// 해소됐다. 대회가 이미 ENDED로 끝난 뒤에는 상금이 stale 값 기준으로 확정돼버릴 수 있으므로,
// 대회가 CALCULATING/ENDED로 넘어가기 전에 자동으로 몇 번 더 재시도해서 스스로 회복할 기회를 준다.
// app.scheduling.enabled=false(벤치마크 프로필 등)일 때만 비활성화 - 값이 없으면(matchIfMissing)
// 기존과 동일하게 항상 켜짐. 실서비스 등 다른 프로필엔 영향 없음.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class FailedRecalculationRetryScheduler {

    // 상장폐지 등으로 영구히 실패하는 건까지 매시간 KIS를 계속 두들기지 않도록 상한을 둔다.
    private static final int MAX_AUTO_RETRY_COUNT = 5;

    private final FailedRecalculationRepository failedRecalculationRepository;
    private final FailedRecalculationService failedRecalculationService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void retryUnresolvedFailures() {
        List<FailedRecalculation> targets =
                failedRecalculationRepository.findByResolvedFalseAndRetryCountLessThan(MAX_AUTO_RETRY_COUNT);
        if (targets.isEmpty()) {
            return;
        }

        log.info("재평가 실패 건 자동 재시도 시작: {}건", targets.size());
        for (FailedRecalculation failure : targets) {
            failedRecalculationService.retryAutomatically(failure.getId());
        }
    }
}
