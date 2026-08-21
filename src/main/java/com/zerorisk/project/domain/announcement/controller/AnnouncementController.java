package com.zerorisk.project.domain.announcement.controller;

import com.zerorisk.project.domain.announcement.dto.AnnouncementResponse;
import com.zerorisk.project.domain.announcement.service.AnnouncementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementService.getAnnouncements();
    }
}
