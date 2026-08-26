package com.zerorisk.project.domain.portfolio.repository;

import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    boolean existsByAccountIdAndSnapshotDate(Long accountId, LocalDate snapshotDate);

    List<PortfolioSnapshot> findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long accountId, LocalDate from, LocalDate to);

    List<PortfolioSnapshot> findAllBySnapshotDate(LocalDate snapshotDate);
}