package com.zerorisk.project.domain.post.dto;

import com.zerorisk.project.domain.post.entity.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostCreateRequest(
        @NotNull BoardType boardType,
        Long stockId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        List<String> imageUrls) {
}
