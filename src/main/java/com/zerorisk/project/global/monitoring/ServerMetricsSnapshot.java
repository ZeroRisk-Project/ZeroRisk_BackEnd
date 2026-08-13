package com.zerorisk.project.global.monitoring;

import java.util.List;

public record ServerMetricsSnapshot(
        long latestResponseTimeMs,
        long averageResponseTimeMs,
        long uptimeSeconds,
        List<MetricPoint> points) {
}
