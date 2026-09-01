package com.zerorisk.project.domain.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRetry() {
        meterRegistry.counter("notification.sse.retry").increment();
    }

    public void recordDlqSaved() {
        meterRegistry.counter("notification.sse.dlq.saved").increment();
    }

    public void recordDlqResolved() {
        meterRegistry.counter("notification.sse.dlq.resolved").increment();
    }

    public void recordDlqIgnored() {
        meterRegistry.counter("notification.sse.dlq.ignored").increment();
    }
}
