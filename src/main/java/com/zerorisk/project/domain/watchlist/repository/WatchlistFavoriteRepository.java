package com.zerorisk.project.domain.watchlist.repository;

import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistFavoriteRepository extends JpaRepository<WatchlistFavorite, Long> {
    boolean existsByGroupIdAndStockId(Long groupId, Long stockId);

    List<WatchlistFavorite> findByUserId(Long userId);

    List<WatchlistFavorite> findByGroupId(Long groupId);
}