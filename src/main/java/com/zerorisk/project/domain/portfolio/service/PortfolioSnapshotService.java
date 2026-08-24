package com.zerorisk.project.domain.portfolio.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioSnapshotResponse;
import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import com.zerorisk.project.domain.portfolio.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final AccountRepository accountRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

    @Transactional(readOnly = true)
    public List<PortfolioSnapshotResponse> getSnapshots(Long userId, Long accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusMonths(1);

        return portfolioSnapshotRepository
                .findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(accountId, resolvedFrom, resolvedTo)
                .stream()
                .map(PortfolioSnapshotResponse::from)
                .toList();
    }

    @Transactional
    public void createDailySnapshots() {
        LocalDate today = LocalDate.now();
        List<Account> accounts = accountRepository.findAll();

        int created = 0;
        for (Account account : accounts) {
            if (portfolioSnapshotRepository.existsByAccountIdAndSnapshotDate(account.getId(), today)) {
                continue;
            }

            BigDecimal cash = account.getBalance();
            BigDecimal stockValue = BigDecimal.ZERO;

            portfolioSnapshotRepository.save(PortfolioSnapshot.builder()
                    .accountId(account.getId())
                    .snapshotDate(today)
                    .cash(cash)
                    .stockValue(stockValue)
                    .build());
            created++;
        }

        log.info("일별 자산 스냅샷 생성 완료: {}건", created);
    }
}