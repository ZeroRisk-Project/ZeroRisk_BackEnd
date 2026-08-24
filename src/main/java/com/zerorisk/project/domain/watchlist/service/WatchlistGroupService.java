package com.zerorisk.project.domain.watchlist.service;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupUpdateRequest;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.exception.WatchlistErrorCode;
import com.zerorisk.project.domain.watchlist.exception.WatchlistException;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<WatchlistGroupResponse> getGroups(Long userId) {
        return watchlistGroupRepository.findByUserId(userId).stream()
                .map(WatchlistGroupResponse::from)
                .toList();
    }

    @Transactional
    public WatchlistGroupResponse updateGroup(Long userId, Long groupId, WatchlistGroupUpdateRequest request) {
        WatchlistGroup group = findOwnedGroup(userId, groupId);
        group.rename(request.name());

        return WatchlistGroupResponse.from(group);
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        WatchlistGroup group = findOwnedGroup(userId, groupId);
        watchlistGroupRepository.delete(group);
    }

    private WatchlistGroup findOwnedGroup(Long userId, Long groupId) {
        WatchlistGroup group = watchlistGroupRepository.findById(groupId)
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.GROUP_NOT_FOUND));

        if (!group.getUserId().equals(userId)) {
            throw new WatchlistException(WatchlistErrorCode.ACCESS_DENIED);
        }

        return group;
    }
}