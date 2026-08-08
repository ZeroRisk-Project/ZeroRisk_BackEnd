package com.zerorisk.project.domain.portfolio.repository;

import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    boolean existsByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);
}