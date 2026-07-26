package com.zerorisk.project.domain.competition.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;

public interface CompetitionRankingRepository extends JpaRepository<CompetitionParticipant, Long> {

    @Query(value = """
            SELECT
                RANK() OVER (ORDER BY cp.RETURN_RATE DESC, cp.TOTAL_ASSET DESC) AS rankPosition,
                cp.USER_ID AS userId,
                u.NICKNAME AS nickname,
                cp.RETURN_RATE AS returnRate,
                cp.TOTAL_ASSET AS totalAsset
            FROM COMPETITION_PARTICIPANTS cp
            JOIN USERS u ON u.ID = cp.USER_ID
            WHERE cp.COMPETITION_ID = :competitionId
            ORDER BY rankPosition
            """, nativeQuery = true)
    List<CompetitionRankingProjection> findRankingsByCompetitionId(@Param("competitionId") Long competitionId);
}