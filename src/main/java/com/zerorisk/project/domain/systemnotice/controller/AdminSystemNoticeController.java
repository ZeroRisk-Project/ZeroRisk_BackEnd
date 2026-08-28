package com.zerorisk.project.domain.systemnotice.controller;

import com.zerorisk.project.domain.systemnotice.dto.SystemNoticeCreateRequest;
import com.zerorisk.project.domain.systemnotice.dto.SystemNoticeResponse;
import com.zerorisk.project.domain.systemnotice.service.SystemNoticeService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system-notices")
@RequiredArgsConstructor
public class AdminSystemNoticeController {
    private final SystemNoticeService systemNoticeService;

    @GetMapping
    public List<SystemNoticeResponse> getAll() {
        return systemNoticeService.getAllForAdmin();
    }

    @PostMapping
    public ResponseEntity<Long> create(@CurrentUserId Long adminId, @Valid @RequestBody SystemNoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemNoticeService.create(adminId, request));
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivate(@CurrentUserId Long adminId, @PathVariable Long id) {
        systemNoticeService.deactivate(adminId, id);
    }
}
