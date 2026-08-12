package com.zerorisk.project.domain.inquiry.repository;

import com.zerorisk.project.domain.inquiry.entity.InquiryStatus;
import java.time.LocalDateTime;

public interface InquiryAdminProjection {
    Long getId();
    String getAuthorNickname();
    String getCategory();
    String getTitle();
    String getContent();
    String getAnswer();
    InquiryStatus getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getAnsweredAt();
}
