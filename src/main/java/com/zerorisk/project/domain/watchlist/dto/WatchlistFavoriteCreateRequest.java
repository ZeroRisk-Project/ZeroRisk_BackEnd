package com.zerorisk.project.domain.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WatchlistFavoriteCreateRequest(
        @NotNull(message = "그룹을 지정해야 합니다.")
        Long groupId,

        @NotBlank(message = "종목 코드를 입력해야 합니다.")
        String stockCode) {
}