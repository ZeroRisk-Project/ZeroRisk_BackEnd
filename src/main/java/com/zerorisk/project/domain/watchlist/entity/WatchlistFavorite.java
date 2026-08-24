package com.zerorisk.project.domain.watchlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "WATCHLIST_FAVORITES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "watchlist_favorites_seq")
    @SequenceGenerator(name = "watchlist_favorites_seq", sequenceName = "WATCHLIST_FAVORITES_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "GROUP_ID", nullable = false)
    private Long groupId;

    @Column(name = "STOCK_ID", nullable = false)
    private Long stockId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WatchlistFavorite(Long userId, Long groupId, Long stockId) {
        this.userId = userId;
        this.groupId = groupId;
        this.stockId = stockId;
        this.createdAt = LocalDateTime.now();
    }
}