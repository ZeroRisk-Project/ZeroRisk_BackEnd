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
import java.util.List;
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

        BigDecimal totalAsset = account.getBalance().add(stockValue);
        BigDecimal initialSeedMoney = account.getInitialSeedMoney();

        BigDecimal returnRate = initialSeedMoney.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalAsset.subtract(initialSeedMoney)
                        .divide(initialSeedMoney, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        participant.updateAsset(totalAsset, returnRate);
    }

    // maxAttempts(3) 모두 실패한 경우에만 호출됨 - 실패를 DLQ에 격리하고 정상 흐름은 계속 진행시킨다.
    @Recover
    public void recoverFromRecalculationFailure(IllegalStateException e, CompetitionParticipant participant) {
        log.warn("참가자 자산 재평가 최종 실패(재시도 3회 소진) - participantId: {}, reason: {}",
                participant.getId(), e.getMessage());
        failedRecalculationService.saveFailure(participant.getId(), e.getMessage());
    }
}
