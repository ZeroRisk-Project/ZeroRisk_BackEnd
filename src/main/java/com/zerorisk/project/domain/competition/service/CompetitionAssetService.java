package com.zerorisk.project.domain.competition.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.competition.entity.CompetitionParticipant;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CompetitionAssetService {

    // PortfolioSnapshotService와 동일한 KIS 호출 간격 제한 (오상민님 도메인 상수와 값만 맞춤 - 공유 대상 아님)
    private static final long KIS_QUOTE_REQUEST_INTERVAL_MILLIS = 600;

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;
    private final FailedRecalculationService failedRecalculationService;

    // FailedRecalculationService.retryResolve()가 다시 이 서비스의 recalculate()를 호출하는
    // 양방향 의존 관계라, 즉시 주입 시 순환참조가 생긴다. @Lazy로 실제 실패 시점까지 주입을 미룬다.
    public CompetitionAssetService(
            AccountRepository accountRepository,
            HoldingRepository holdingRepository,
            StockRepository stockRepository,
            KisQuoteClient kisQuoteClient,
            @Lazy FailedRecalculationService failedRecalculationService) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.stockRepository = stockRepository;
        this.kisQuoteClient = kisQuoteClient;
        this.failedRecalculationService = failedRecalculationService;
    }

    // KIS 시세 조회 실패(일시적 장애성)만 재시도 대상으로 삼는다. AccountException/StockNotFoundException은
    // 데이터 자체가 잘못된 경우라 재시도해도 의미가 없어서 제외 - KisQuoteClientImpl을 직접 확인한 결과
    // 실제로 던지는 예외는 IllegalStateException("KIS 시세 조회에 실패했습니다...") 하나뿐이다.
    @Retryable(
            retryFor = { IllegalStateException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2, random = true),
            listeners = "recalculationRetryListener"
    )
    @Transactional
    public void recalculate(CompetitionParticipant participant) {
        Account account = accountRepository.findById(participant.getAccountId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        List<Holding> holdings = holdingRepository.findByAccountId(account.getId());

        BigDecimal stockValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            Stock stock = stockRepository.findById(holding.getStockId())
                    .orElseThrow(StockNotFoundException::new);
            BigDecimal currentPrice = new BigDecimal(kisQuoteClient.fetchQuote(stock.getCode()).currentPrice());
            stockValue = stockValue.add(currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity())));
        }

        applyStockValue(participant, account, stockValue);
    }

    // maxAttempts(3) 모두 실패한 경우에만 호출됨 - 실패를 DLQ에 격리하고 정상 흐름은 계속 진행시킨다.
    @Recover
    public void recoverFromRecalculationFailure(IllegalStateException e, CompetitionParticipant participant) {
        log.warn("참가자 자산 재평가 최종 실패(재시도 3회 소진) - participantId: {}, reason: {}",
                participant.getId(), e.getMessage());
        failedRecalculationService.saveFailure(participant.getId(), e.getMessage());
    }

    // 대회 상금 지급(distributePrizes)처럼 참가자 전원을 한꺼번에 재평가할 때 쓴다. participant마다
    // recalculate()를 그대로 호출하면 같은 종목을 참가자 수만큼 중복으로 KIS 조회하게 되므로,
    // PortfolioSnapshotService.fetchCurrentPrices()와 동일하게 필요한 종목 시세를 먼저 한 번에
    // 캐시해둔다. 종목 하나라도 일괄 조회에 실패하면 그 종목을 보유한 참가자는 캐시에서 빠지고,
    // 호출 측(CompetitionService)이 hasAllPricesCached()로 걸러서 기존 recalculate()로 폴백한다.
    public Map<Long, BigDecimal> prefetchPrices(List<CompetitionParticipant> participants) {
        Map<Long, List<Holding>> holdingsByAccountId = new HashMap<>();
        for (CompetitionParticipant participant : participants) {
            holdingsByAccountId.put(participant.getAccountId(),
                    holdingRepository.findByAccountId(participant.getAccountId()));
        }

        Set<Long> stockIds = holdingsByAccountId.values().stream()
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
                log.warn("종목 {} 현재가 일괄 조회 실패 - 이 종목을 보유한 참가자는 개별 재조회(recalculate)로 폴백합니다.",
                        stock.getCode(), e);
            }

            sleepForThrottle();
        }

        return priceByStockId;
    }

    // priceByStockId에 이 참가자가 보유한 모든 종목의 시세가 들어있는지 확인한다.
    // false면 recalculateFromCache() 대신 기존 recalculate()로 폴백해야 한다.
    public boolean hasAllPricesCached(CompetitionParticipant participant, Map<Long, BigDecimal> priceByStockId) {
        return holdingRepository.findByAccountId(participant.getAccountId()).stream()
                .allMatch(holding -> priceByStockId.containsKey(holding.getStockId()));
    }

    // prefetchPrices()로 미리 캐시해둔 시세만 사용 - KIS를 다시 호출하지 않는다.
    // hasAllPricesCached()가 true인 참가자에게만 호출할 것 (호출 전 반드시 확인).
    @Transactional
    public void recalculateFromCache(CompetitionParticipant participant, Map<Long, BigDecimal> priceByStockId) {
        Account account = accountRepository.findById(participant.getAccountId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        List<Holding> holdings = holdingRepository.findByAccountId(account.getId());
        BigDecimal stockValue = holdings.stream()
                .map(holding -> priceByStockId.get(holding.getStockId()).multiply(BigDecimal.valueOf(holding.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        applyStockValue(participant, account, stockValue);
    }

    private void applyStockValue(CompetitionParticipant participant, Account account, BigDecimal stockValue) {
        BigDecimal totalAsset = account.getBalance().add(stockValue);
        BigDecimal initialSeedMoney = account.getInitialSeedMoney();

        BigDecimal returnRate = initialSeedMoney.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalAsset.subtract(initialSeedMoney)
                        .divide(initialSeedMoney, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        participant.updateAsset(totalAsset, returnRate);
    }

    private void sleepForThrottle() {
        try {
            Thread.sleep(KIS_QUOTE_REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
