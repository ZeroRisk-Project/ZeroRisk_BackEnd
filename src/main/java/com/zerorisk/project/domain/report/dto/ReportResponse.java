package com.zerorisk.project.domain.report.dto;

import com.zerorisk.project.domain.report.entity.Report;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.entity.TargetType;
import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        Long reporterId,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReporter().getId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt());
    }
}