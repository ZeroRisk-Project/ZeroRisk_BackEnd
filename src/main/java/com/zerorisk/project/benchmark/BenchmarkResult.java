package com.zerorisk.project.benchmark;

public record BenchmarkResult(
        String readerType,
        int dataVolume,
        long avgExecutionTimeMs,
        double degradationRatio) { // 데이터 규모가 가장 작을 때(기준) 대비 몇 배 느려졌는지
}
