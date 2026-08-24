package com.zerorisk.project.domain.watchlist.service;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistGroupService {

    private final WatchlistGroupRepository watchlistGroupRepository;

    @Transactional
    public WatchlistGroupResponse createGroup(Long userId, WatchlistGroupCreateRequest request) {
        WatchlistGroup group = WatchlistGroup.builder()
                .userId(userId)
                .name(request.name())
                .build();
        watchlistGroupRepository.save(group);

        return WatchlistGroupResponse.from(group);
    }
}