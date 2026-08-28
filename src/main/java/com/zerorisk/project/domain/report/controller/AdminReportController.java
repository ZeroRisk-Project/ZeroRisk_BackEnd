package com.zerorisk.project.domain.report.controller;

import com.zerorisk.project.domain.report.dto.ReportProcessRequest;
import com.zerorisk.project.domain.report.dto.ReportResponse;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.service.ReportService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public Page<ReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable) {
        return reportService.getReports(status, pageable);
    }

    @PatchMapping("/{reportId}")
    public ReportResponse processReport(
            @CurrentUserId Long adminId,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportProcessRequest request) {
        return reportService.processReport(adminId, reportId, request);
    }
}