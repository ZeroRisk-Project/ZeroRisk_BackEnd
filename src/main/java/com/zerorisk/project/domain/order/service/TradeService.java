package com.zerorisk.project.domain.order.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.order.dto.TradeResponse;
import com.zerorisk.project.domain.order.entity.Trade;
import com.zerorisk.project.domain.order.repository.TradeRepository;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Page<TradeResponse> getTrades(Long userId, Long accountId, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        Page<Trade> trades = tradeRepository.findByAccountId(accountId, pageable);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        trades.getContent().stream().map(Trade::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return trades.map(trade -> TradeResponse.of(trade, stocksById.get(trade.getStockId())));
    }
}