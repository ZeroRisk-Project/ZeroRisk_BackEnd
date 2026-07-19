package com.zerorisk.project.domain.inquiry.controller;

import com.zerorisk.project.domain.inquiry.dto.InquiryCreateRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryResponse;
import com.zerorisk.project.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(@Valid @RequestBody InquiryCreateRequest request) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 userId 주입
        Long userId = 1L;

        InquiryResponse response = inquiryService.createInquiry(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<InquiryResponse>> getMyInquiries(Pageable pageable) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 userId 주입
        Long userId = 1L;

        return ResponseEntity.ok(inquiryService.getMyInquiries(userId, pageable));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryResponse> getInquiry(@PathVariable Long inquiryId) {
        // TODO: JWT 로그인 완료 후 @AuthenticationPrincipal로 실제 userId 주입
        Long userId = 1L;

        return ResponseEntity.ok(inquiryService.getInquiry(userId, inquiryId));
    }
}