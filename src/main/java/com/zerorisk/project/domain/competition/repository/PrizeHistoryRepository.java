package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeHistoryRepository extends JpaRepository<PrizeHistory, Long> {
    List<PrizeHistory> findByUserId(Long userId);
}