package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.Competition;
import com.zerorisk.project.domain.competition.entity.CompetitionStatus;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Page<Competition> findByIsPublicTrue(Pageable pageable);

    List<Competition> findByStatus(CompetitionStatus status);

    List<Competition> findByStatusAndStartAtBefore(CompetitionStatus status, LocalDateTime now);

    List<Competition> findByStatusAndEndAtBefore(CompetitionStatus status, LocalDateTime now);
}