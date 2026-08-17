package com.zerorisk.project.domain.competition.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionParticipantRepository extends JpaRepository<CompetitionParticipant, Long> {
    Optional<CompetitionParticipant> findByCompetitionIdAndUserId(Long competitionId, Long userId);

    @Query(value = """
            SELECT
                cp.RANK_POSITION AS rank,
                u.NICKNAME AS nickname,
                cp.RETURN_RATE AS returnRate,
                cp.TOTAL_ASSET AS totalAsset,
                ph.PRIZE_AMOUNT AS prizeAmount
            FROM COMPETITION_PARTICIPANTS cp
            JOIN USERS u ON u.ID = cp.USER_ID
            LEFT JOIN PRIZE_HISTORY ph ON ph.COMPETITION_ID = cp.COMPETITION_ID AND ph.USER_ID = cp.USER_ID
            WHERE cp.COMPETITION_ID = :competitionId
            ORDER BY cp.RANK_POSITION ASC
            """, nativeQuery = true)
    List<CompetitionArchiveProjection> findArchiveByCompetitionId(@Param("competitionId") Long competitionId);

    List<CompetitionParticipant> findByUserId(Long userId);

    long countByCompetitionId(Long competitionId);

    @Query("""
            SELECT cp.competitionId AS competitionId, COUNT(cp) AS count
            FROM CompetitionParticipant cp
            WHERE cp.competitionId IN :competitionIds
            GROUP BY cp.competitionId
            """)
    List<CompetitionParticipantCountProjection> countByCompetitionIds(@Param("competitionIds") List<Long> competitionIds);

    @Query("""
            SELECT cp.userId AS userId, u.nickname AS nickname, u.email AS email,
                   cp.joinedAt AS joinedAt, cp.returnRate AS returnRate, cp.totalAsset AS totalAsset
            FROM CompetitionParticipant cp
            JOIN User u ON u.id = cp.userId
            WHERE cp.competitionId = :competitionId
            ORDER BY cp.joinedAt ASC
            """)
    List<CompetitionParticipantAdminProjection> findParticipantsWithUserInfo(@Param("competitionId") Long competitionId);

    @Query("""
            SELECT cp.competitionId AS competitionId, c.title AS title,
                   cp.rankPosition AS rankPosition, cp.returnRate AS returnRate
            FROM CompetitionParticipant cp
            JOIN Competition c ON c.id = cp.competitionId
            WHERE cp.userId = :userId
            ORDER BY cp.joinedAt DESC
            """)
    List<ProfileCompetitionProjection> findCompetitionHistoryByUserId(@Param("userId") Long userId);
}