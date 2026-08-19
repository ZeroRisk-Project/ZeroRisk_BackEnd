package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;

    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        List<Holding> holdings = holdingRepository.findByAccountId(accountId);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        holdings.stream().map(Holding::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return holdings.stream()
                .map(holding -> {
                    Stock stock = stocksById.get(holding.getStockId());
                    BigDecimal currentPrice = fetchCurrentPrice(stock.getCode());
                    return HoldingResponse.of(holding, stock, currentPrice);
                })
                .toList();
    }

    private BigDecimal fetchCurrentPrice(String code) {
        return new BigDecimal(kisQuoteClient.fetchQuote(code).currentPrice());
    }
}