package com.zerorisk.project.domain.portfolio.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import com.zerorisk.project.domain.portfolio.repository.PortfolioSnapshotRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    private PortfolioSnapshotService portfolioSnapshotService;

    @DisplayName("당일 스냅샷이 없는 계좌는 새로 생성")
    @Test
    void 당일_스냅샷이_없는_계좌는_새로_생성() {
        portfolioSnapshotService = new PortfolioSnapshotService(accountRepository, portfolioSnapshotRepository);
        Account account = Account.builder()
                .userId(1L)
                .accountType(AccountType.BASIC)
                .build();
        given(accountRepository.findAll()).willReturn(List.of(account));
        given(portfolioSnapshotRepository.existsByAccountIdAndSnapshotDate(account.getId(), LocalDate.now()))
                .willReturn(false);

        portfolioSnapshotService.createDailySnapshots();

        verify(portfolioSnapshotRepository, times(1)).save(any(PortfolioSnapshot.class));
    }

    @DisplayName("당일 스냅샷이 이미 있는 계좌는 건너뜀")
    @Test
    void 당일_스냅샷이_이미_있는_계좌는_건너뜀() {
        portfolioSnapshotService = new PortfolioSnapshotService(accountRepository, portfolioSnapshotRepository);
        Account account = Account.builder()
                .userId(1L)
                .accountType(AccountType.BASIC)
                .build();
        given(accountRepository.findAll()).willReturn(List.of(account));
        given(portfolioSnapshotRepository.existsByAccountIdAndSnapshotDate(account.getId(), LocalDate.now()))
                .willReturn(true);

        portfolioSnapshotService.createDailySnapshots();

        verify(portfolioSnapshotRepository, never()).save(any(PortfolioSnapshot.class));
    }
}