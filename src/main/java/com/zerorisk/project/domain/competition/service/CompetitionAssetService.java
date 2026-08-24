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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompetitionAssetService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;

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
}
