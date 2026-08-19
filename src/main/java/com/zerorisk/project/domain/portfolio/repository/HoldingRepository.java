package com.zerorisk.project.domain.portfolio.repository;

import com.zerorisk.project.domain.portfolio.entity.Holding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByAccountIdAndStockId(Long accountId, Long stockId);
    List<Holding> findByAccountId(Long accountId);
}