package com.zerorisk.project.domain.watchlist.repository;

import com.zerorisk.project.domain.watchlist.entity.WatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {

}