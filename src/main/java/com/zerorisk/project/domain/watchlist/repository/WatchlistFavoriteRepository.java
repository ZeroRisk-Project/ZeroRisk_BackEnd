package com.zerorisk.project.domain.watchlist.repository;

import com.zerorisk.project.domain.watchlist.entity.WatchlistFavorite;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchlistFavoriteRepository extends JpaRepository<WatchlistFavorite, Long> {
    boolean existsByGroupIdAndStockId(Long groupId, Long stockId);

    List<WatchlistFavorite> findByUserId(Long userId);

    List<WatchlistFavorite> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    // 종목 인기 랭킹: 전체 사용자의 관심종목 등록 건수를 종목별로 집계해 많은 순으로 반환한다.
    @Query("select wf.stockId as stockId, count(wf) as favoriteCount "
            + "from WatchlistFavorite wf group by wf.stockId order by count(wf) desc")
    List<StockFavoriteCount> countGroupedByStockDesc(Pageable pageable);

    interface StockFavoriteCount {
        Long getStockId();

        Long getFavoriteCount();
    }
}