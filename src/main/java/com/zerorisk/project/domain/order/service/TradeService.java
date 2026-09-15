package com.zerorisk.project.domain.order.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.order.dto.TradeResponse;
import com.zerorisk.project.domain.order.entity.Trade;
import com.zerorisk.project.domain.order.repository.TradeRepository;
import com.zerorisk.project.domain.profile.dto.ProfileResponse.TradeSummary;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 프로필 공개용 - 소유권 검증 없이 targetUserId로 서버가 직접 계좌를 찾으므로 IDOR 우려 없음.
    // 수량/가격은 반환하지 않고 종목명·매매구분·시간만 반환한다 (기획: 타인 프로필에는 상세 금액 노출 안 함).
    @Transactional(readOnly = true)
    public List<TradeSummary> getRecentTradesForUserId(Long targetUserId, int limit) {
        Optional<Account> accountOpt = accountRepository.findByUserIdAndAccountType(targetUserId, AccountType.BASIC);
        if (accountOpt.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "tradedAt"));
        Page<Trade> trades = tradeRepository.findByAccountId(accountOpt.get().getId(), pageable);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        trades.getContent().stream().map(Trade::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return trades.getContent().stream()
                .map(trade -> new TradeSummary(
                        stocksById.get(trade.getStockId()).getName(),
                        trade.getSide(),
                        trade.getTradedAt()))
                .toList();
    }
}