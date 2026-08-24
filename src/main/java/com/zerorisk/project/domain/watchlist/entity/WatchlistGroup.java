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
@Table(name = "WATCHLIST_GROUPS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "watchlist_groups_seq")
    @SequenceGenerator(name = "watchlist_groups_seq", sequenceName = "WATCHLIST_GROUPS_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WatchlistGroup(Long userId, String name) {
        this.userId = userId;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public void rename(String name) {
        this.name = name;
    }
}