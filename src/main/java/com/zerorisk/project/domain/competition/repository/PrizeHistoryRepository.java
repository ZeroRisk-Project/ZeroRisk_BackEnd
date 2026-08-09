package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.PrizeHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrizeHistoryRepository extends JpaRepository<PrizeHistory, Long> {

    @Query("""
            SELECT ph.competitionId AS competitionId, c.title AS competitionTitle,
                   ph.rankPosition AS rankPosition, ph.prizeAmount AS prizeAmount, ph.paidAt AS paidAt
            FROM PrizeHistory ph
            JOIN Competition c ON c.id = ph.competitionId
            WHERE ph.userId = :userId
            ORDER BY ph.paidAt DESC
            """)
    List<MyPrizeHistoryProjection> findByUserIdWithCompetitionTitle(@Param("userId") Long userId);
}
