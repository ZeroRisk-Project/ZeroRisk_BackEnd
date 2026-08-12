package com.zerorisk.project.domain.inquiry.dto;

import com.zerorisk.project.domain.inquiry.entity.InquiryStatus;
import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long id,
        String authorNickname,
        String category,
        String title,
        String content,
        String answer,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt) {
}
