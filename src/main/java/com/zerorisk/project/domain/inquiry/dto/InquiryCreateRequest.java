package com.zerorisk.project.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotBlank @Size(max = 30) String category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content) {
}