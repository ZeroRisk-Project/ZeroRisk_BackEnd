package com.zerorisk.project.global.monitoring;

public record DashboardSummaryResponse(
        long totalUserCount,
        long todayNewUserCount,
        long pendingReportCount,
        long pendingInquiryCount,
        long latestResponseTimeMs,
        long averageResponseTimeMs,
        long uptimeSeconds) {
}
