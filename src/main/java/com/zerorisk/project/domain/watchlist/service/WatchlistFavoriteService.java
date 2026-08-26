package com.zerorisk.project.domain.watchlist.service;

import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteCreateRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteMoveRequest;
import com.zerorisk.project.domain.watchlist.dto.WatchlistFavoriteResponse;
import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import com.zerorisk.project.domain.watchlist.exception.WatchlistErrorCode;
import com.zerorisk.project.domain.watchlist.exception.WatchlistException;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistGroupRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchlistFavoriteService {

    private final WatchlistFavoriteRepository watchlistFavoriteRepository;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final StockRepository stockRepository;

    @Transactional
    public WatchlistFavoriteResponse addFavorite(Long userId, WatchlistFavoriteCreateRequest request) {
        WatchlistGroup group = watchlistGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.GROUP_NOT_FOUND));

        if (!group.getUserId().equals(userId)) {
            throw new WatchlistException(WatchlistErrorCode.ACCESS_DENIED);
        }

        Stock stock = stockRepository.findByCode(request.stockCode())
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        if (watchlistFavoriteRepository.existsByGroupIdAndStockId(group.getId(), stock.getId())) {
            throw new WatchlistException(WatchlistErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        WatchlistFavorite favorite = WatchlistFavorite.builder()
                .userId(userId)
                .groupId(group.getId())
                .stockId(stock.getId())
                .build();
        watchlistFavoriteRepository.save(favorite);

        return WatchlistFavoriteResponse.of(favorite, stock);
    }

    @Transactional
    public void removeFavorite(Long userId, Long favoriteId) {
        WatchlistFavorite favorite = findOwnedFavorite(userId, favoriteId);
        watchlistFavoriteRepository.delete(favorite);
    }

    @Transactional
    public WatchlistFavoriteResponse moveFavorite(Long userId, Long favoriteId, WatchlistFavoriteMoveRequest request) {
        WatchlistFavorite favorite = findOwnedFavorite(userId, favoriteId);

        WatchlistGroup targetGroup = watchlistGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.GROUP_NOT_FOUND));

        if (!targetGroup.getUserId().equals(userId)) {
            throw new WatchlistException(WatchlistErrorCode.ACCESS_DENIED);
        }

        if (watchlistFavoriteRepository.existsByGroupIdAndStockId(targetGroup.getId(), favorite.getStockId())) {
            throw new WatchlistException(WatchlistErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        favorite.moveToGroup(targetGroup.getId());

        Stock stock = stockRepository.findById(favorite.getStockId())
                .orElseThrow(StockNotFoundException::new);

        return WatchlistFavoriteResponse.of(favorite, stock);
    }

    private WatchlistFavorite findOwnedFavorite(Long userId, Long favoriteId) {
        WatchlistFavorite favorite = watchlistFavoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new WatchlistException(WatchlistErrorCode.FAVORITE_NOT_FOUND));

        if (!favorite.getUserId().equals(userId)) {
            throw new WatchlistException(WatchlistErrorCode.ACCESS_DENIED);
        }

        return favorite;
    }
}