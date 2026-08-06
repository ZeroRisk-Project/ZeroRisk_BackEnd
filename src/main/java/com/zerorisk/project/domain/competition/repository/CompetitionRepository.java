package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Page<Competition> findByIsPublicTrue(Pageable pageable);

    List<Competition> findByStatus(CompetitionStatus status);

    List<Competition> findByStatusAndStartAtBefore(CompetitionStatus status, LocalDateTime now);

    List<Competition> findByStatusAndEndAtBefore(CompetitionStatus status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Competition c WHERE c.id = :id")
    Optional<Competition> findByIdForUpdate(@Param("id") Long id);
}