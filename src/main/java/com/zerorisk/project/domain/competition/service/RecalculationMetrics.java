package com.zerorisk.project.domain.competition.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecalculationMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRetry() {
        meterRegistry.counter("competition.recalculation.retry").increment();
    }

    public void recordDlqSaved() {
        meterRegistry.counter("competition.recalculation.dlq.saved").increment();
    }

    public void recordDlqResolved() {
        meterRegistry.counter("competition.recalculation.dlq.resolved").increment();
    }

    public void recordCompletionTime(long millis) {
        meterRegistry.timer("competition.recalculation.completion.time").record(millis, TimeUnit.MILLISECONDS);
    }
}
