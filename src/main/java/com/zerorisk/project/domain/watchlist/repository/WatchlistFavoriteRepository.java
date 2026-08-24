package com.zerorisk.project.domain.watchlist.repository;

import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistFavoriteRepository extends JpaRepository<WatchlistFavorite, Long> {
    boolean existsByGroupIdAndStockId(Long groupId, Long stockId);
}