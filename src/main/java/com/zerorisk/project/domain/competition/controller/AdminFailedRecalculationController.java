package com.zerorisk.project.domain.competition.controller;

import com.zerorisk.project.domain.competition.dto.FailedRecalculationResponse;
import com.zerorisk.project.domain.competition.service.FailedRecalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/admin/** 하위라 SecurityConfig의 일괄 규칙(.hasRole("ADMIN"))으로 이미 보호됨 - 별도 어노테이션 불필요.
@RestController
@RequestMapping("/api/v1/admin/failed-recalculations")
@RequiredArgsConstructor
public class AdminFailedRecalculationController {

    private final FailedRecalculationService failedRecalculationService;

    @GetMapping
    public Page<FailedRecalculationResponse> getAll(
            @RequestParam(defaultValue = "false") boolean resolved,
            Pageable pageable) {
        return failedRecalculationService.getFailures(resolved, pageable)
                .map(FailedRecalculationResponse::from);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long id) {
        failedRecalculationService.retryResolve(id);
        return ResponseEntity.noContent().build();
    }
}
