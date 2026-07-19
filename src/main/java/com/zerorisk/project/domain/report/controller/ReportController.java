package com.zerorisk.project.domain.report.controller;

import com.zerorisk.project.domain.report.dto.ReportCreateRequest;
import com.zerorisk.project.domain.report.dto.ReportResponse;
import com.zerorisk.project.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportCreateRequest request) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 reporterId 주입
        Long reporterId = 1L;

        ReportResponse response = reportService.createReport(reporterId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}