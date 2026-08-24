package com.zerorisk.project.domain.portfolio.controller;

import com.zerorisk.project.domain.portfolio.dto.PortfolioCompositionResponse;
import com.zerorisk.project.domain.portfolio.dto.PortfolioSnapshotResponse;
import com.zerorisk.project.domain.portfolio.service.PortfolioCompositionService;
import com.zerorisk.project.domain.portfolio.service.PortfolioSnapshotService;
import com.zerorisk.project.global.security.CurrentUserId;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioCompositionService portfolioCompositionService;
    private final PortfolioSnapshotService portfolioSnapshotService;

    @GetMapping("/composition")
    public ResponseEntity<PortfolioCompositionResponse> getComposition(
            @CurrentUserId Long userId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(portfolioCompositionService.getComposition(userId, accountId));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<PortfolioSnapshotResponse>> getSnapshots(
            @CurrentUserId Long userId,
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(portfolioSnapshotService.getSnapshots(userId, accountId, from, to));
    }
}