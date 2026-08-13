package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/action-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActionLogController {

    private final AdminActionLogService adminActionLogService;

    @GetMapping
    public Page<AdminActionLogResponse> getLogs(Pageable pageable) {
        return adminActionLogService.getLogs(pageable);
    }
}
