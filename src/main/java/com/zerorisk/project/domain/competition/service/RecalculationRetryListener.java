package com.zerorisk.project.domain.competition.service;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

// CompetitionAssetService.recalculate()의 재시도(@Retryable) 시도마다 지표를 기록하기 위한 리스너.
// 비즈니스 로직(recalculate) 안에 카운팅 코드를 직접 넣지 않기 위해 분리함.
@Component("recalculationRetryListener")
@RequiredArgsConstructor
public class RecalculationRetryListener implements RetryListener {

    private final RecalculationMetrics recalculationMetrics;

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        recalculationMetrics.recordRetry();
    }
}
