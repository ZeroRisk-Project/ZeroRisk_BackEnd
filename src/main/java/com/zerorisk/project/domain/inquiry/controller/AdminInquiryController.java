package com.zerorisk.project.domain.inquiry.controller;

import com.zerorisk.project.domain.inquiry.dto.AdminInquiryResponse;
import com.zerorisk.project.domain.inquiry.dto.InquiryAnswerRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryResponse;
import com.zerorisk.project.domain.inquiry.service.InquiryService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public Page<AdminInquiryResponse> getAllInquiries(Pageable pageable) {
        return inquiryService.getAllInquiriesForAdmin(pageable);
    }

    @PostMapping("/{inquiryId}/answer")
    public InquiryResponse answerInquiry(
            @CurrentUserId Long adminId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        return inquiryService.answerInquiry(adminId, inquiryId, request);
    }
}