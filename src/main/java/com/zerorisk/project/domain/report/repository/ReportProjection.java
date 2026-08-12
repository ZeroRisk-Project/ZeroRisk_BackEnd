package com.zerorisk.project.domain.report.repository;

import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.entity.TargetType;
import java.time.LocalDateTime;

public interface ReportProjection {
    Long getId();
    TargetType getTargetType();
    Long getTargetId();
    String getReporterNickname();
    String getReason();
    ReportStatus getStatus();
    LocalDateTime getCreatedAt();
}
