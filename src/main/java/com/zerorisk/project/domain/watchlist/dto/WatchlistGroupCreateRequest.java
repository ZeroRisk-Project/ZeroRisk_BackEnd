package com.zerorisk.project.domain.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistGroupCreateRequest(
        @NotBlank(message = "그룹 이름을 입력해야 합니다.")
        @Size(max = 50, message = "그룹 이름은 50자를 초과할 수 없습니다.")
        String name) {
}