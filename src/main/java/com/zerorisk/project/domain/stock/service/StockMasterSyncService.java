package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.cache.StockAliasCache;
import com.zerorisk.project.domain.stock.client.kis.KisStockMasterClient;
import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class StockMasterSyncService {

    private final KisStockMasterClient kisStockMasterClient;
    private final StockRepository stockRepository;
    private final StockAliasCache stockAliasCache;
    private final TransactionTemplate requiresNewTransaction;

    public StockMasterSyncService(
            KisStockMasterClient kisStockMasterClient,
            StockRepository stockRepository,
            StockAliasCache stockAliasCache,
            PlatformTransactionManager transactionManager) {
        this.kisStockMasterClient = kisStockMasterClient;
        this.stockRepository = stockRepository;
        this.stockAliasCache = stockAliasCache;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // 종목별로 독립된 트랜잭션에 저장한다 - 마스터 파일의 특정 종목 한 건이 저장에 실패해도
    // (컬럼 길이 초과 등) 나머지 수천 건이 통째로 롤백되어 전체 상세 조회가 막히는 일이 없도록 한다.
    public void sync() {
        List<StockMasterRow> rows = kisStockMasterClient.fetchAll();
        if (rows.isEmpty()) {
            log.warn("KIS 종목 마스터 데이터가 비어 있어 동기화를 건너뜁니다.");
            return;
        }

        Set<String> syncedCodes = new HashSet<>();
        int created = 0;
        int updated = 0;
        int failed = 0;

        for (StockMasterRow row : rows) {
            if (!syncedCodes.add(row.code())) {
                continue;
            }
            try {
                boolean wasCreated = Boolean.TRUE.equals(requiresNewTransaction.execute(status -> upsert(row)));
                if (wasCreated) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("종목 마스터 동기화 중 저장 실패: code={}, name={}", row.code(), row.name(), e);
            }
        }

        int deactivated = deactivateMissing(syncedCodes);

        stockAliasCache.reload();

        log.info("종목 마스터 동기화 완료: 신규 {}건, 갱신 {}건, 저장 실패 {}건, 비활성화 {}건, 별칭 캐시 {}건",
                created, updated, failed, deactivated, stockAliasCache.size());
    }

    private boolean upsert(StockMasterRow row) {
        Stock stock = stockRepository.findByCode(row.code()).orElse(null);
        if (stock == null) {
            stockRepository.save(Stock.builder()
                    .code(row.code())
                    .standardCode(row.standardCode())
                    .name(row.name())
                    .market(row.market())
                    .sectorCode(row.sectorCode())
                    .build());
            return true;
        }
        stock.updateFrom(row.name(), row.standardCode(), row.market(), row.sectorCode());
        return false;
    }

    private int deactivateMissing(Set<String> syncedCodes) {
        List<Long> toDeactivate = stockRepository.findAll().stream()
                .filter(Stock::getActive)
                .filter(stock -> !syncedCodes.contains(stock.getCode()))
                .map(Stock::getId)
                .toList();

        int deactivated = 0;
        for (Long id : toDeactivate) {
            try {
                requiresNewTransaction.executeWithoutResult(
                        status -> stockRepository.findById(id).ifPresent(Stock::deactivate));
                deactivated++;
            } catch (Exception e) {
                log.warn("종목 비활성화 중 오류가 발생했습니다: id={}", id, e);
            }
        }
        return deactivated;
    }
}
