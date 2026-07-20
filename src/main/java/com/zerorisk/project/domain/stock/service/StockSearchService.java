package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.cache.StockAliasCache;
import com.zerorisk.project.domain.stock.dto.StockSummaryResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final StockRepository stockRepository;
    private final StockAliasCache stockAliasCache;

    @Transactional(readOnly = true)
    public Page<StockSummaryResponse> search(String keyword, Pageable pageable) {
        Optional<Stock> aliasedStock = stockAliasCache.resolve(keyword)
                .flatMap(stockRepository::findByCode);

        if (aliasedStock.isPresent()) {
            return new PageImpl<>(List.of(StockSummaryResponse.from(aliasedStock.get())), pageable, 1);
        }

        return stockRepository.search(keyword, pageable)
                .map(StockSummaryResponse::from);
    }
}