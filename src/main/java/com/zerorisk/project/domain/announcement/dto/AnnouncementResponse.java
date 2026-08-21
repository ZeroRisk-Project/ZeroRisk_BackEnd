package com.zerorisk.project.domain.announcement.dto;

import com.zerorisk.project.domain.announcement.entity.Announcement;
import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id, String tag, String title, String content, boolean isImportant, LocalDateTime createdAt
) {
    public static AnnouncementResponse from(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTag().name(), a.getTitle(), a.getContent(), a.getIsImportant(), a.getCreatedAt());
    }
}
