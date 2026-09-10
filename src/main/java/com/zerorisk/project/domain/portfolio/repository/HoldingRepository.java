package com.zerorisk.project.domain.portfolio.repository;

import com.zerorisk.project.domain.portfolio.entity.Holding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByAccountIdAndStockId(Long accountId, Long stockId);
    List<Holding> findByAccountId(Long accountId);

    // 일별 종가 배치가 KIS를 조회할 종목 범위를 정하는 데 쓴다 - 계좌 종류(BASIC/COMPETITION)와
    // 무관하게 실제로 누군가 보유 중인 종목만 대상으로 삼는다.
    @Query("select distinct h.stockId from Holding h")
    List<Long> findDistinctStockIds();
}