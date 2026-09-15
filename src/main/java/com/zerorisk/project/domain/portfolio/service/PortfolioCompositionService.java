package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
import com.zerorisk.project.domain.portfolio.dto.PortfolioCompositionResponse;
import com.zerorisk.project.domain.portfolio.dto.StockCompositionItem;
import com.zerorisk.project.domain.profile.dto.ProfileResponse.PortfolioSummary;
import com.zerorisk.project.domain.profile.dto.ProfileResponse.StockWeightItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
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

    // 프로필 공개용 - 소유권 검증 없이 targetUserId로 서버가 직접 계좌를 찾으므로 IDOR 우려 없음.
    // 절대 금액은 반환하지 않고 비중(%)만 반환한다 (기획: 타인 프로필에는 현금/종목 비중만 노출).
    @Transactional(readOnly = true)
    public PortfolioSummary getCompositionSummaryForUserId(Long targetUserId) {
        Optional<Account> accountOpt = accountRepository.findByUserIdAndAccountType(targetUserId, AccountType.BASIC);
        if (accountOpt.isEmpty()) {
            return new PortfolioSummary(BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }

        Account account = accountOpt.get();
        List<HoldingResponse> holdings = holdingService.getHoldings(account.getId());

        BigDecimal cash = account.getBalance();
        BigDecimal stockValue = holdings.stream()
                .map(HoldingResponse::evaluationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAsset = cash.add(stockValue);

        List<StockWeightItem> stocks = holdings.stream()
                .map(holding -> new StockWeightItem(
                        holding.stockName(),
                        ratio(holding.evaluationAmount(), totalAsset)))
                .toList();

        return new PortfolioSummary(ratio(cash, totalAsset), ratio(stockValue, totalAsset), stocks);
    }

    private BigDecimal ratio(BigDecimal amount, BigDecimal totalAsset) {
        if (totalAsset.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(totalAsset, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}