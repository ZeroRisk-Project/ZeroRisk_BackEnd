package com.zerorisk.project.global.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class ServerMetrics {

    private static final int MAX_SAMPLES = 60; // 최근 60개 포인트 (1분 간격이면 1시간)

    private record TimedSample(LocalDateTime timestamp, long responseTimeMs) {}

    private final ConcurrentLinkedDeque<TimedSample> samples = new ConcurrentLinkedDeque<>();
    private final long startedAt = System.currentTimeMillis();

    public void record(long elapsedMs) {
        samples.addLast(new TimedSample(LocalDateTime.now(), elapsedMs));
        while (samples.size() > MAX_SAMPLES) {
            samples.pollFirst();
        }
    }

    public ServerMetricsSnapshot snapshot() {
        if (samples.isEmpty()) {
            return new ServerMetricsSnapshot(0, 0, uptimeSeconds(), List.of());
        }

        long sum = 0;
        long latest = 0;
        for (TimedSample s : samples) {
            sum += s.responseTimeMs();
            latest = s.responseTimeMs();
        }
        long average = sum / samples.size();

        List<MetricPoint> points = samples.stream()
                .map(s -> new MetricPoint(s.timestamp(), s.responseTimeMs()))
                .toList();

        return new ServerMetricsSnapshot(latest, average, uptimeSeconds(), points);
    }

    private long uptimeSeconds() {
        return (System.currentTimeMillis() - startedAt) / 1000;
    }
}
