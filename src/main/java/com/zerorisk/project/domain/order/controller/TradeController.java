package com.zerorisk.project.domain.order.controller;

import com.zerorisk.project.domain.order.dto.TradeResponse;
import com.zerorisk.project.domain.order.service.TradeService;
import com.zerorisk.project.global.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public ResponseEntity<Page<TradeResponse>> getTrades(
            @CurrentUserId Long userId,
            @RequestParam Long accountId,
            Pageable pageable) {
        return ResponseEntity.ok(tradeService.getTrades(userId, accountId, pageable));
    }
}