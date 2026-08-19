package com.zerorisk.project.domain.portfolio.controller;

import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
import com.zerorisk.project.domain.portfolio.service.HoldingService;
import com.zerorisk.project.global.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getHoldings(
            @CurrentUserId Long userId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(holdingService.getHoldings(userId, accountId));
    }
}