package com.zerorisk.project.domain.report.dto;

import com.zerorisk.project.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportProcessRequest(
                @NotNull ReportStatus status) {
}