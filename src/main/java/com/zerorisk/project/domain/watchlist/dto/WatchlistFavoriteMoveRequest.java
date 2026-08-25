package com.zerorisk.project.domain.watchlist.dto;

import jakarta.validation.constraints.NotNull;

public record WatchlistFavoriteMoveRequest(
        @NotNull(message = "이동할 그룹을 지정해야 합니다.")
        Long groupId) {
}