package com.zerorisk.project.domain.order.repository;

import com.zerorisk.project.domain.order.dto.AccountTradeCountRow;
import com.zerorisk.project.domain.order.entity.Trade;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Page<Trade> findByAccountId(Long accountId, Pageable pageable);

    @Query("""
            SELECT new com.zerorisk.project.domain.order.dto.AccountTradeCountRow(t.accountId, COUNT(t))
            FROM Trade t
            WHERE t.accountId IN :accountIds
            GROUP BY t.accountId
            """)
    List<AccountTradeCountRow> countByAccountIdIn(@Param("accountIds") Collection<Long> accountIds);
}