package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionParticipantRepository extends JpaRepository<CompetitionParticipant, Long> {
    Optional<CompetitionParticipant> findByCompetitionIdAndUserId(Long competitionId, Long userId);
}