package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
import com.zerorisk.project.domain.portfolio.dto.PortfolioCompositionResponse;
import com.zerorisk.project.domain.portfolio.dto.StockCompositionItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioCompositionService {

    private final HoldingService holdingService;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public PortfolioCompositionResponse getComposition(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        List<HoldingResponse> holdings = holdingService.getHoldings(userId, accountId);

        BigDecimal cash = account.getBalance();
        BigDecimal stockValue = holdings.stream()
                .map(HoldingResponse::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAsset = cash.add(stockValue);

        List<StockCompositionItem> stocks = holdings.stream()
                .map(holding -> new StockCompositionItem(
                        holding.stockCode(),
                        holding.stockName(),
                        holding.evaluationAmount(),
                        ratio(holding.evaluationAmount(), totalAsset)))
                .toList();

        return new PortfolioCompositionResponse(
                cash,
                stockValue,
                totalAsset,
                ratio(cash, totalAsset),
                ratio(stockValue, totalAsset),
                stocks);
    }

    private BigDecimal ratio(BigDecimal amount, BigDecimal totalAsset) {
        if (totalAsset.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(totalAsset, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}