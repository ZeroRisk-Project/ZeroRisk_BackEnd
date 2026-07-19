package com.zerorisk.project.domain.inquiry.controller;

import com.zerorisk.project.domain.inquiry.dto.InquiryAnswerRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryResponse;
import com.zerorisk.project.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: SecurityConfig에서 /api/v1/admin/** 경로에 ADMIN 권한 체크 적용 필요 (박종권님 설정 확인)
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/{inquiryId}/answer")
    public InquiryResponse answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        return inquiryService.answerInquiry(inquiryId, request);
    }
}