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
        // POST_IMAGES.IMAGE_URL이 VARCHAR2(500)이라, 검증 없이 그대로 저장하면 게시글 생성 자체가
        // DB 제약 위반으로 롤백된다 - 입력 단계에서 먼저 걸러서 명확한 400을 돌려준다.
        List<@Size(max = 500) String> imageUrls) {
}
