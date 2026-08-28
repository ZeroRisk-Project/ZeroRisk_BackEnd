package com.zerorisk.project.global.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
public class ServerMetricsController {

    private final ServerMetrics serverMetrics;

    @GetMapping
    public ServerMetricsSnapshot getMetrics() {
        return serverMetrics.snapshot();
    }
}
