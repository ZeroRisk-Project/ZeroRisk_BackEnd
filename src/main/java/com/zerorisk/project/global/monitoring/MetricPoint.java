package com.zerorisk.project.global.monitoring;

import java.time.LocalDateTime;

public record MetricPoint(
        LocalDateTime timestamp,
        long responseTimeMs) {
}
