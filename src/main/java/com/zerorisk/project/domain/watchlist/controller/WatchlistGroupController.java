package com.zerorisk.project.domain.watchlist.controller;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.service.WatchlistGroupService;
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
@RequestMapping("/api/v1/watchlist/groups")
@RequiredArgsConstructor
public class WatchlistGroupController {

    private final WatchlistGroupService watchlistGroupService;

    @PostMapping
    public ResponseEntity<WatchlistGroupResponse> createGroup(
            @CurrentUserId Long userId,
            @Valid @RequestBody WatchlistGroupCreateRequest request) {
        WatchlistGroupResponse response = watchlistGroupService.createGroup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}