package com.zerorisk.project.domain.watchlist.controller;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupUpdateRequest;
import com.zerorisk.project.domain.watchlist.service.WatchlistGroupService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public ResponseEntity<List<WatchlistGroupResponse>> getGroups(@CurrentUserId Long userId) {
        return ResponseEntity.ok(watchlistGroupService.getGroups(userId));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<WatchlistGroupResponse> updateGroup(
            @CurrentUserId Long userId,
            @PathVariable Long groupId,
            @Valid @RequestBody WatchlistGroupUpdateRequest request) {
        return ResponseEntity.ok(watchlistGroupService.updateGroup(userId, groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @CurrentUserId Long userId,
            @PathVariable Long groupId) {
        watchlistGroupService.deleteGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}