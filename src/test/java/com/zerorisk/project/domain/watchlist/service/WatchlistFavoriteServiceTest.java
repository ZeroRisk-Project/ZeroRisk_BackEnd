package com.zerorisk.project.domain.watchlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteMoveRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteResponse;
import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.exception.WatchlistException;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WatchlistFavoriteServiceTest {

    @Mock
    private WatchlistFavoriteRepository watchlistFavoriteRepository;

    @Mock
    private WatchlistGroupRepository watchlistGroupRepository;

    @Mock
    private StockRepository stockRepository;

    private WatchlistFavoriteService watchlistFavoriteService;

    private WatchlistGroup group(Long userId) {
        return WatchlistGroup.builder()
                .userId(userId)
                .name("반도체")
                .build();
    }

    private Stock stock() {
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        ReflectionTestUtils.setField(stock, "id", 1L);
        return stock;
    }

    private WatchlistFavorite favorite(Long userId) {
        return WatchlistFavorite.builder()
                .userId(userId)
                .groupId(10L)
                .stockId(1L)
                .build();
    }

    @DisplayName("관심종목을 그룹에 추가")
    @Test
    void 관심종목을_그룹에_추가() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(1L)));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        given(watchlistFavoriteRepository.existsByGroupIdAndStockId(null, 1L)).willReturn(false);

        WatchlistFavoriteCreateRequest request = new WatchlistFavoriteCreateRequest(10L, "005930");
        WatchlistFavoriteResponse response = watchlistFavoriteService.addFavorite(1L, request);

        assertThat(response.stockCode()).isEqualTo("005930");
    }

    @DisplayName("다른 사용자의 그룹에 관심종목을 추가할 시 예외 발생")
    @Test
    void 다른_사용자의_그룹에_관심종목을_추가하려_할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(2L)));

        WatchlistFavoriteCreateRequest request = new WatchlistFavoriteCreateRequest(10L, "005930");

        assertThatThrownBy(() -> watchlistFavoriteService.addFavorite(1L, request))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("존재하지 않는 종목을 추가할 시 예외 발생")
    @Test
    void 존재하지_않는_종목을_추가할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(1L)));
        given(stockRepository.findByCode("999999")).willReturn(Optional.empty());

        WatchlistFavoriteCreateRequest request = new WatchlistFavoriteCreateRequest(10L, "999999");

        assertThatThrownBy(() -> watchlistFavoriteService.addFavorite(1L, request))
                .isInstanceOf(StockNotFoundException.class);
    }

    @DisplayName("이미 그룹에 추가된 종목을 다시 추가할 시 예외 발생")
    @Test
    void 이미_그룹에_추가된_종목을_다시_추가할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistGroupRepository.findById(10L)).willReturn(Optional.of(group(1L)));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        given(watchlistFavoriteRepository.existsByGroupIdAndStockId(null, 1L)).willReturn(true);

        WatchlistFavoriteCreateRequest request = new WatchlistFavoriteCreateRequest(10L, "005930");

        assertThatThrownBy(() -> watchlistFavoriteService.addFavorite(1L, request))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("관심종목을 삭제")
    @Test
    void 관심종목을_삭제() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        WatchlistFavorite favorite = favorite(1L);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.of(favorite));

        watchlistFavoriteService.removeFavorite(1L, 20L);

        verify(watchlistFavoriteRepository).delete(favorite);
    }

    @DisplayName("다른 사용자의 관심종목을 삭제할 시 예외 발생")
    @Test
    void 다른_사용자의_관심종목을_삭제할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.of(favorite(2L)));

        assertThatThrownBy(() -> watchlistFavoriteService.removeFavorite(1L, 20L))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("존재하지 않는 관심종목을 삭제할 시 예외 발생")
    @Test
    void 존재하지_않는_관심종목을_삭제할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistFavoriteService.removeFavorite(1L, 20L))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("관심종목을 다른 그룹으로 이동")
    @Test
    void 관심종목을_다른_그룹으로_이동() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        WatchlistFavorite favorite = favorite(1L);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.of(favorite));
        given(watchlistGroupRepository.findById(11L)).willReturn(Optional.of(group(1L)));
        given(watchlistFavoriteRepository.existsByGroupIdAndStockId(null, 1L)).willReturn(false);
        given(stockRepository.findById(1L)).willReturn(Optional.of(stock()));

        WatchlistFavoriteMoveRequest request = new WatchlistFavoriteMoveRequest(11L);
        WatchlistFavoriteResponse response = watchlistFavoriteService.moveFavorite(1L, 20L, request);

        assertThat(response.stockCode()).isEqualTo("005930");
    }

    @DisplayName("이미 대상 그룹에 있는 종목으로 이동할 시 예외 발생")
    @Test
    void 이미_대상_그룹에_있는_종목으로_이동할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        WatchlistFavorite favorite = favorite(1L);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.of(favorite));
        given(watchlistGroupRepository.findById(11L)).willReturn(Optional.of(group(1L)));
        given(watchlistFavoriteRepository.existsByGroupIdAndStockId(null, 1L)).willReturn(true);

        WatchlistFavoriteMoveRequest request = new WatchlistFavoriteMoveRequest(11L);

        assertThatThrownBy(() -> watchlistFavoriteService.moveFavorite(1L, 20L, request))
                .isInstanceOf(WatchlistException.class);
    }

    @DisplayName("다른 사용자의 그룹으로 관심종목을 이동할 시 예외 발생")
    @Test
    void 다른_사용자의_그룹으로_관심종목을_이동할_시_예외_발생() {
        watchlistFavoriteService = new WatchlistFavoriteService(
                watchlistFavoriteRepository, watchlistGroupRepository, stockRepository);
        WatchlistFavorite favorite = favorite(1L);
        given(watchlistFavoriteRepository.findById(20L)).willReturn(Optional.of(favorite));
        given(watchlistGroupRepository.findById(11L)).willReturn(Optional.of(group(2L)));

        WatchlistFavoriteMoveRequest request = new WatchlistFavoriteMoveRequest(11L);

        assertThatThrownBy(() -> watchlistFavoriteService.moveFavorite(1L, 20L, request))
                .isInstanceOf(WatchlistException.class);
    }
}