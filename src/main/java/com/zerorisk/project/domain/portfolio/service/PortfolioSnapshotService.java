package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioSnapshotResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.portfolio.repository.PortfolioSnapshotRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
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
public class PortfolioSnapshotService {

    private static final long KIS_QUOTE_REQUEST_INTERVAL_MILLIS = 600;

    private final AccountRepository accountRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;

    @Transactional(readOnly = true)
    public List<PortfolioSnapshotResponse> getSnapshots(Long userId, Long accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusMonths(1);

        return portfolioSnapshotRepository
                .findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(accountId, resolvedFrom, resolvedTo)
                .stream()
                .map(PortfolioSnapshotResponse::from)
                .toList();
    }

    @Transactional
    public void createDailySnapshots() {
        LocalDate today = LocalDate.now();
        List<Account> targetAccounts = accountRepository.findAll().stream()
                .filter(account -> !portfolioSnapshotRepository.existsByAccountIdAndSnapshotDate(account.getId(), today))
                .toList();

        Map<Long, List<Holding>> holdingsByAccountId = new HashMap<>();
        for (Account account : targetAccounts) {
            holdingsByAccountId.put(account.getId(), holdingRepository.findByAccountId(account.getId()));
        }

        Map<Long, BigDecimal> priceByStockId = fetchCurrentPrices(holdingsByAccountId.values());
        int created = 0;
        for (Account account : targetAccounts) {
            BigDecimal stockValue = holdingsByAccountId.get(account.getId()).stream()
                    .map(holding -> priceByStockId.getOrDefault(holding.getStockId(), BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(holding.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            portfolioSnapshotRepository.save(PortfolioSnapshot.builder()
                    .accountId(account.getId())
                    .snapshotDate(today)
                    .cash(account.getBalance())
                    .stockValue(stockValue)
                    .build());
            created++;
        }

        log.info("일별 자산 스냅샷 생성 완료: {}건", created);
    }

    private Map<Long, BigDecimal> fetchCurrentPrices(Collection<List<Holding>> holdingLists) {
        Set<Long> stockIds = holdingLists.stream()
                .flatMap(List::stream)
                .map(Holding::getStockId)
                .collect(Collectors.toSet());

        if (stockIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Stock> stocksById = stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        Map<Long, BigDecimal> priceByStockId = new HashMap<>();
        for (Long stockId : stockIds) {
            Stock stock = stocksById.get(stockId);
            if (stock == null) {
                continue;
            }

            try {
                priceByStockId.put(stockId, new BigDecimal(kisQuoteClient.fetchQuote(stock.getCode()).currentPrice()));
            } catch (Exception e) {
                log.warn("종목 {} 현재가 조회에 실패하여 이번 스냅샷에서는 0으로 반영합니다.", stock.getCode(), e);
            }

            sleepForThrottle();
        }

        return priceByStockId;
    }

    private void sleepForThrottle() {
        try {
            Thread.sleep(KIS_QUOTE_REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}