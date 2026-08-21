package com.zerorisk.project.domain.systemnotice.controller;

import com.zerorisk.project.domain.systemnotice.dto.SystemNoticeResponse;
import com.zerorisk.project.domain.systemnotice.service.SystemNoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-notices")
@RequiredArgsConstructor
public class SystemNoticeController {
    private final SystemNoticeService systemNoticeService;

    @GetMapping("/active")
    public List<SystemNoticeResponse> getActiveNotices() {
        return systemNoticeService.getActiveNotices();
    }
}
