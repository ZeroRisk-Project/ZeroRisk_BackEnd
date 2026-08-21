package com.zerorisk.project.domain.announcement.controller;

import com.zerorisk.project.domain.announcement.dto.AnnouncementRequest;
import com.zerorisk.project.domain.announcement.service.AnnouncementService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {
    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<Long> create(@CurrentUserId Long adminId, @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.create(adminId, request));
    }

    @PutMapping("/{id}")
    public void update(@CurrentUserId Long adminId, @PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        announcementService.update(adminId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@CurrentUserId Long adminId, @PathVariable Long id) {
        announcementService.delete(adminId, id);
    }
}
