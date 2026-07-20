package com.zerorisk.project.domain.stock.controller;

import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.dto.StockSummaryResponse;
import com.zerorisk.project.domain.stock.service.StockQueryService;
import com.zerorisk.project.domain.stock.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryService stockQueryService;
    private final StockSearchService stockSearchService;

    @GetMapping("/search")
    public ResponseEntity<Page<StockSummaryResponse>> search(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(stockSearchService.search(keyword, pageable));
    }

    @GetMapping("/{code}")
    public ResponseEntity<StockDetailResponse> getDetail(@PathVariable String code) {
        return ResponseEntity.ok(stockQueryService.getDetail(code));
    }
}