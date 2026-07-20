package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.cache.StockAliasCache;
import com.zerorisk.project.domain.stock.client.kis.KisStockMasterClient;
import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterSyncService {

    private final KisStockMasterClient kisStockMasterClient;
    private final StockRepository stockRepository;
    private final StockAliasCache stockAliasCache;

    @Transactional
    public void sync() {
        List<StockMasterRow> rows = kisStockMasterClient.fetchAll();
        if (rows.isEmpty()) {
            log.warn("KIS 종목 마스터 데이터가 비어 있어 동기화를 건너뜁니다.");
            return;
        }

        Map<String, Stock> existingByCode = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getCode, Function.identity()));

        Set<String> syncedCodes = new HashSet<>();
        int created = 0;
        int updated = 0;

        for (StockMasterRow row : rows) {
            if (!syncedCodes.add(row.code())) {
                continue;
            }

            Stock stock = existingByCode.get(row.code());
            if (stock == null) {
                stockRepository.save(Stock.builder()
                        .code(row.code())
                        .standardCode(row.standardCode())
                        .name(row.name())
                        .market(row.market())
                        .build());
                created++;
            } else {
                stock.updateFrom(row.name(), row.standardCode(), row.market());
                updated++;
            }
        }

        int deactivated = 0;
        for (Stock stock : existingByCode.values()) {
            if (stock.getActive() && !syncedCodes.contains(stock.getCode())) {
                stock.deactivate();
                deactivated++;
            }
        }

        stockAliasCache.reload();

        log.info("종목 마스터 동기화 완료: 신규 {}건, 갱신 {}건, 비활성화 {}건, 별칭 캐시 {}건",
                created, updated, deactivated, stockAliasCache.size());
    }
}