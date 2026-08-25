package com.zerorisk.project.domain.watchlist.controller;

import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteResponse;
import com.zerorisk.project.domain.watchlist.service.WatchlistFavoriteService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/watchlist/favorites")
@RequiredArgsConstructor
public class WatchlistFavoriteController {

    private final WatchlistFavoriteService watchlistFavoriteService;

    @PostMapping
    public ResponseEntity<WatchlistFavoriteResponse> addFavorite(
            @CurrentUserId Long userId,
            @Valid @RequestBody WatchlistFavoriteCreateRequest request) {
        WatchlistFavoriteResponse response = watchlistFavoriteService.addFavorite(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}