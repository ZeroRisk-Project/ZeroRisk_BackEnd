package com.zerorisk.project.domain.watchlist.dto;

import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import java.time.LocalDateTime;

public record WatchlistFavoriteResponse(
        Long favoriteId,
        Long groupId,
        String stockCode,
        String stockName,
        LocalDateTime createdAt) {

    public static WatchlistFavoriteResponse of(WatchlistFavorite favorite, Stock stock) {
        return new WatchlistFavoriteResponse(
                favorite.getId(),
                favorite.getGroupId(),
                stock.getCode(),
                stock.getName(),
                favorite.getCreatedAt());
    }
}