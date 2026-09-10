package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioRiskResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.MarketIndexDailyPrice;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockDailyPrice;
import com.zerorisk.project.domain.stock.repository.MarketIndexDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 베타/변동성을 표준 정의(공분산 행렬 기반) 그대로 계산한다. 개별 종목 변동성의 단순
// 가중평균을 쓰지 않는 이유: 그러면 분산투자 효과(종목 간 상관관계)가 반영되지 않아
// 실제보다 변동성이 과대평가된다.
@Service
@RequiredArgsConstructor
public class PortfolioRiskService {

    private static final int LOOKBACK_DAYS = 100;
    private static final int MIN_COMMON_DATES = 20;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final StockDailyPriceRepository stockDailyPriceRepository;
    private final MarketIndexDailyPriceRepository marketIndexDailyPriceRepository;
    private final KisQuoteClient kisQuoteClient;

    @Transactional(readOnly = true)
    public PortfolioRiskResponse getRisk(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));
        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        List<Holding> holdings = holdingRepository.findByAccountId(accountId);
        if (holdings.isEmpty()) {
            return PortfolioRiskResponse.unavailable();
        }

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        holdings.stream().map(Holding::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        Map<Long, BigDecimal> evaluationByStockId = new LinkedHashMap<>();
        for (Holding holding : holdings) {
            Stock stock = stocksById.get(holding.getStockId());
            if (stock == null) {
                continue;
            }
            BigDecimal price = new BigDecimal(kisQuoteClient.fetchQuote(stock.getCode()).currentPrice());
            evaluationByStockId.put(holding.getStockId(), price.multiply(BigDecimal.valueOf(holding.getQuantity())));
        }

        BigDecimal totalValue = evaluationByStockId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return PortfolioRiskResponse.unavailable();
        }

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(LOOKBACK_DAYS);

        Map<LocalDate, BigDecimal> indexByDate = marketIndexDailyPriceRepository
                .findByMarketAndPriceDateBetweenOrderByPriceDateAsc(Market.KOSPI, from, today)
                .stream()
                .collect(Collectors.toMap(MarketIndexDailyPrice::getPriceDate, MarketIndexDailyPrice::getCloseValue));

        Map<Long, Map<LocalDate, BigDecimal>> priceByStockAndDate = new LinkedHashMap<>();
        for (Long stockId : evaluationByStockId.keySet()) {
            Map<LocalDate, BigDecimal> byDate = stockDailyPriceRepository
                    .findByStockIdAndPriceDateBetweenOrderByPriceDateAsc(stockId, from, today)
                    .stream()
                    .collect(Collectors.toMap(StockDailyPrice::getPriceDate, StockDailyPrice::getClosePrice));
            priceByStockAndDate.put(stockId, byDate);
        }

        TreeSet<LocalDate> commonDates = new TreeSet<>(indexByDate.keySet());
        for (Map<LocalDate, BigDecimal> byDate : priceByStockAndDate.values()) {
            commonDates.retainAll(byDate.keySet());
        }

        if (commonDates.size() < MIN_COMMON_DATES) {
            return PortfolioRiskResponse.unavailable();
        }

        List<LocalDate> sortedDates = new ArrayList<>(commonDates);
        double[] indexReturns = dailyReturns(sortedDates.stream().map(indexByDate::get).toList());

        List<Long> stockIds = new ArrayList<>(evaluationByStockId.keySet());
        double[] weights = new double[stockIds.size()];
        double[][] stockReturns = new double[stockIds.size()][];
        for (int i = 0; i < stockIds.size(); i++) {
            Long stockId = stockIds.get(i);
            weights[i] = evaluationByStockId.get(stockId).divide(totalValue, 10, RoundingMode.HALF_UP).doubleValue();
            Map<LocalDate, BigDecimal> byDate = priceByStockAndDate.get(stockId);
            stockReturns[i] = dailyReturns(sortedDates.stream().map(byDate::get).toList());
        }

        double indexVariance = variance(indexReturns);
        double portfolioBeta = 0;
        for (int i = 0; i < stockIds.size(); i++) {
            double betaI = indexVariance == 0 ? 0 : covariance(stockReturns[i], indexReturns) / indexVariance;
            portfolioBeta += weights[i] * betaI;
        }

        double portfolioVariance = 0;
        for (int i = 0; i < stockIds.size(); i++) {
            for (int j = 0; j < stockIds.size(); j++) {
                double cov = i == j ? variance(stockReturns[i]) : covariance(stockReturns[i], stockReturns[j]);
                portfolioVariance += weights[i] * weights[j] * cov;
            }
        }
        double portfolioVolatility = Math.sqrt(Math.max(portfolioVariance, 0) * TRADING_DAYS_PER_YEAR) * 100;

        return new PortfolioRiskResponse(true, round(portfolioBeta), round(portfolioVolatility));
    }

    private double[] dailyReturns(List<BigDecimal> prices) {
        double[] returns = new double[prices.size() - 1];
        for (int i = 1; i < prices.size(); i++) {
            double prev = prices.get(i - 1).doubleValue();
            double curr = prices.get(i).doubleValue();
            returns[i - 1] = prev == 0 ? 0 : (curr - prev) / prev;
        }
        return returns;
    }

    private double mean(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private double variance(double[] values) {
        return covariance(values, values);
    }

    private double covariance(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        if (n == 0) {
            return 0;
        }
        double meanA = mean(a);
        double meanB = mean(b);
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += (a[i] - meanA) * (b[i] - meanB);
        }
        return sum / n;
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
