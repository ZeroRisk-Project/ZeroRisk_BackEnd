package com.zerorisk.project.domain.inquiry.dto;

import com.zerorisk.project.domain.inquiry.entity.Inquiry;
import com.zerorisk.project.domain.inquiry.entity.InquiryStatus;
import java.time.LocalDateTime;

public record InquiryResponse(
        Long id,
        String category,
        String title,
        String content,
        String answer,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt) {

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getAnsweredAt());
    }
}