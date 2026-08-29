package com.zerorisk.project.domain.competition.repository;

import com.zerorisk.project.domain.competition.entity.FailedRecalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedRecalculationRepository extends JpaRepository<FailedRecalculation, Long> {
    Page<FailedRecalculation> findByResolved(boolean resolved, Pageable pageable);
}
