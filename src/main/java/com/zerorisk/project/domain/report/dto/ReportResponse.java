package com.zerorisk.project.domain.report.dto;

import com.zerorisk.project.domain.report.entity.Report;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.entity.TargetType;
import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        String reporterNickname,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt,
        Long targetPostId) {

    public static ReportResponse from(Report report, Long targetPostId) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReporter().getNickname(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                targetPostId);
    }
}
