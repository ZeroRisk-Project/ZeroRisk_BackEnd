package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisChartClient;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockDailyPrice;
import com.zerorisk.project.domain.stock.repository.StockDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

// 종목마다 KIS 조회 성공/실패가 독립적이라, StockMasterSyncService와 동일하게 종목별로
// 별도 트랜잭션에 저장한다 - 한 종목 저장 실패가 나머지 전체를 롤백시키지 않도록 한다.
@Slf4j
@Service
public class StockDailyPriceService {

    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int BACKFILL_LOOKBACK_DAYS = 100;
    private static final long KIS_REQUEST_INTERVAL_MILLIS = 600;

    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final StockDailyPriceRepository stockDailyPriceRepository;
    private final KisChartClient kisChartClient;
    private final KisQuoteClient kisQuoteClient;
    private final TransactionTemplate requiresNewTransaction;

    public StockDailyPriceService(
            HoldingRepository holdingRepository,
            StockRepository stockRepository,
            StockDailyPriceRepository stockDailyPriceRepository,
            KisChartClient kisChartClient,
            KisQuoteClient kisQuoteClient,
            PlatformTransactionManager transactionManager) {
        this.holdingRepository = holdingRepository;
        this.stockRepository = stockRepository;
        this.stockDailyPriceRepository = stockDailyPriceRepository;
        this.kisChartClient = kisChartClient;
        this.kisQuoteClient = kisQuoteClient;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void createDailyPrices() {
        LocalDate today = LocalDate.now();
        List<Long> stockIds = holdingRepository.findDistinctStockIds();

        int backfilled = 0;
        int appended = 0;
        for (Long stockId : stockIds) {
            if (stockDailyPriceRepository.existsByStockIdAndPriceDate(stockId, today)) {
                continue;
            }
            Stock stock = stockRepository.findById(stockId).orElse(null);
            if (stock == null) {
                continue;
            }

            try {
                boolean isNewlyTracked = !stockDailyPriceRepository.existsByStockId(stockId);
                requiresNewTransaction.executeWithoutResult(status -> {
                    if (isNewlyTracked) {
                        backfillHistory(stock, today);
                    } else {
                        appendToday(stock, today);
                    }
                });
                if (isNewlyTracked) {
                    backfilled++;
                } else {
                    appended++;
                }
            } catch (Exception e) {
                log.warn("종목 {} 일별 종가 저장 실패", stock.getCode(), e);
            }

            sleepForThrottle();
        }

        log.info("종목 일별 종가 배치 완료: 신규 백필 {}건, 당일 추가 {}건", backfilled, appended);
    }

    private void appendToday(Stock stock, LocalDate today) {
        BigDecimal closePrice = new BigDecimal(kisQuoteClient.fetchQuote(stock.getCode()).currentPrice());
        stockDailyPriceRepository.save(StockDailyPrice.builder()
                .stockId(stock.getId())
                .priceDate(today)
                .closePrice(closePrice)
                .build());
    }

    private void backfillHistory(Stock stock, LocalDate today) {
        String startDate = today.minusDays(BACKFILL_LOOKBACK_DAYS).format(KIS_DATE_FORMAT);
        String endDate = today.format(KIS_DATE_FORMAT);

        List<KisDailyChartResponse.Candle> candles =
                kisChartClient.fetchDailyChart(stock.getCode(), "D", startDate, endDate);

        for (KisDailyChartResponse.Candle candle : candles) {
            LocalDate priceDate = LocalDate.parse(candle.date(), KIS_DATE_FORMAT);
            if (stockDailyPriceRepository.existsByStockIdAndPriceDate(stock.getId(), priceDate)) {
                continue;
            }
            stockDailyPriceRepository.save(StockDailyPrice.builder()
                    .stockId(stock.getId())
                    .priceDate(priceDate)
                    .closePrice(new BigDecimal(candle.close()))
                    .build());
        }
    }

    private void sleepForThrottle() {
        try {
            Thread.sleep(KIS_REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
