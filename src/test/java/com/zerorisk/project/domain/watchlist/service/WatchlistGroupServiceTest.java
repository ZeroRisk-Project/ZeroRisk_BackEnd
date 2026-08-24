package com.zerorisk.project.domain.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistGroupServiceTest {

    @Mock
    private WatchlistGroupRepository watchlistGroupRepository;

    private WatchlistGroupService watchlistGroupService;

    @DisplayName("관심 그룹 생성")
    @Test
    void 관심_그룹_생성() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository);
        given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        WatchlistGroupCreateRequest request = new WatchlistGroupCreateRequest("반도체");
        WatchlistGroupResponse response = watchlistGroupService.createGroup(1L, request);

        assertThat(response.name()).isEqualTo("반도체");
    }
}