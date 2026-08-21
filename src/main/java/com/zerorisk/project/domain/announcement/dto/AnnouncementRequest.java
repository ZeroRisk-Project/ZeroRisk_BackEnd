package com.zerorisk.project.domain.announcement.dto;

import com.zerorisk.project.domain.announcement.entity.AnnouncementTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnouncementRequest(
        @NotNull AnnouncementTag tag,
        @NotBlank String title,
        @NotBlank String content,
        Boolean isImportant
) {
}
