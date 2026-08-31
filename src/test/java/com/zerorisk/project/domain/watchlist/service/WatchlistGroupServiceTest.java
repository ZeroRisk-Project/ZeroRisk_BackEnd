package com.zerorisk.project.domain.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupResponse;
import com.zerorisk.project.domain.watchlist.dto.WatchlistGroupUpdateRequest;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.exception.WatchlistException;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WatchlistGroupServiceTest {

    @Mock
    private WatchlistGroupRepository watchlistGroupRepository;

    @Mock
    private WatchlistFavoriteRepository watchlistFavoriteRepository;

    private WatchlistGroupService watchlistGroupService;

    private WatchlistGroup group(Long userId, String name) {
        return WatchlistGroup.builder()
                .userId(userId)
                .name(name)
                .build();
    }

    @DisplayName("관심 그룹 생성")
    @Test
    void 관심_그룹_생성() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.save(any(WatchlistGroup.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        WatchlistGroupCreateRequest request = new WatchlistGroupCreateRequest("반도체");
        WatchlistGroupResponse response = watchlistGroupService.createGroup(1L, request);

        assertThat(response.name()).isEqualTo("반도체");
    }

    @DisplayName("사용자의 관심 그룹 목록 조회")
    @Test
    void 사용자의_관심_그룹_목록_조회() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.findByUserId(1L)).willReturn(List.of(group(1L, "반도체")));

        List<WatchlistGroupResponse> response = watchlistGroupService.getGroups(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).name()).isEqualTo("반도체");
    }

    @DisplayName("관심 그룹의 이름 수정")
    @Test
    void 관심_그룹의_이름_수정() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        WatchlistGroup group = group(1L, "반도체");
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group));

        WatchlistGroupUpdateRequest request = new WatchlistGroupUpdateRequest("2차전지");
        WatchlistGroupResponse response = watchlistGroupService.updateGroup(1L, 10L, request);

        assertThat(response.name()).isEqualTo("2차전지");
    }

    @DisplayName("다른 사용자의 관심 그룹을 수정할 시 예외 발생")
    @Test
    void 다른_사용자의_관심_그룹을_수정할_시_예외_발생() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(2L, "반도체")));

        WatchlistGroupUpdateRequest request = new WatchlistGroupUpdateRequest("2차전지");

        assertThatThrownBy(() -> watchlistGroupService.updateGroup(1L, 10L, request))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("존재하지 않는 관심 그룹을 수정할 시 예외 발생")
    @Test
    void 존재하지_않는_관심_그룹을_수정할_시_예외_발생() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.empty());

        WatchlistGroupUpdateRequest request = new WatchlistGroupUpdateRequest("2차전지");

        assertThatThrownBy(() -> watchlistGroupService.updateGroup(1L, 10L, request))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("관심 그룹 삭제")
    @Test
    void 관심_그룹_삭제() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        WatchlistGroup group = group(1L, "반도체");
        ReflectionTestUtils.setField(group, "id", 10L);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group));

        watchlistGroupService.deleteGroup(1L, 10L);

        verify(watchlistGroupRepository).delete(group);
    }

    @DisplayName("관심 그룹 삭제 시 그룹에 속한 관심종목도 함께 삭제")
    @Test
    void 관심_그룹_삭제_시_그룹에_속한_관심종목도_함께_삭제() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        WatchlistGroup group = group(1L, "반도체");
        ReflectionTestUtils.setField(group, "id", 10L);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group));

        watchlistGroupService.deleteGroup(1L, 10L);

        InOrder inOrder = inOrder(watchlistFavoriteRepository, watchlistGroupRepository);
        inOrder.verify(watchlistFavoriteRepository).deleteByGroupId(10L);
        inOrder.verify(watchlistGroupRepository).delete(group);
    }

    @DisplayName("다른 사용자의 관심 그룹을 삭제할 시 관심종목이 삭제되지 않음")
    @Test
    void 다른_사용자의_관심_그룹을_삭제할_시_관심종목이_삭제되지_않음() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(2L, "반도체")));

        assertThatThrownBy(() -> watchlistGroupService.deleteGroup(1L, 10L))
                .isInstanceOf(WatchlistException.class);

        verify(watchlistFavoriteRepository, never()).deleteByGroupId(any());
    }

    @DisplayName("다른 사용자의 관심 그룹을 삭제할 시 예외 발생")
    @Test
    void 다른_사용자의_관심_그룹을_삭제할_시_예외_발생() {
        watchlistGroupService = new WatchlistGroupService(watchlistGroupRepository, watchlistFavoriteRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(2L, "반도체")));

        assertThatThrownBy(() -> watchlistGroupService.deleteGroup(1L, 10L))
                .isInstanceOf(WatchlistException.class);
    }
}