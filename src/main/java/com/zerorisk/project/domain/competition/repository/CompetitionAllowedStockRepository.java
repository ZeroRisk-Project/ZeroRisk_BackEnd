package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.CompetitionAllowedStock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionAllowedStockRepository extends JpaRepository<CompetitionAllowedStock, Long> {
    List<CompetitionAllowedStock> findByCompetitionId(Long competitionId);
}
