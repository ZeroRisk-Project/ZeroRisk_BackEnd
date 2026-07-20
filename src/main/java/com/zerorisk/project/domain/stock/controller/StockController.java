package com.zerorisk.project.domain.stock.controller;

import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryService stockQueryService;

    @GetMapping("/{code}")
    public ResponseEntity<StockDetailResponse> getDetail(@PathVariable String code) {
        return ResponseEntity.ok(stockQueryService.getDetail(code));
    }
}