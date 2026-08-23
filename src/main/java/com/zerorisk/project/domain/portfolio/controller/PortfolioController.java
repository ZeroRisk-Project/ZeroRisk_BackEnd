package com.zerorisk.project.domain.portfolio.controller;

import com.zerorisk.project.domain.portfolio.dto.PortfolioCompositionResponse;
import com.zerorisk.project.domain.portfolio.service.PortfolioCompositionService;
import com.zerorisk.project.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/composition")
    public ResponseEntity<PortfolioCompositionResponse> getComposition(
            @CurrentUserId Long userId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(portfolioCompositionService.getComposition(userId, accountId));
    }
}