package com.zerorisk.project.domain.watchlist.dto;

import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import java.time.LocalDateTime;

public record WatchlistGroupResponse(
        Long groupId,
        String name,
        LocalDateTime createdAt) {

    public static WatchlistGroupResponse from(WatchlistGroup group) {
        return new WatchlistGroupResponse(
                group.getId(),
                group.getName(),
                group.getCreatedAt());
    }
}