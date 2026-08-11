package com.zerorisk.project.domain.inquiry.controller;

import com.zerorisk.project.domain.inquiry.dto.InquiryAnswerRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryResponse;
import com.zerorisk.project.domain.inquiry.service.InquiryService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/{inquiryId}/answer")
    public InquiryResponse answerInquiry(
            @CurrentUserId Long adminId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        return inquiryService.answerInquiry(adminId, inquiryId, request);
    }
}