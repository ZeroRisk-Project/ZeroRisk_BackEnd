package com.zerorisk.project.domain.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioSnapshotResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.entity.PortfolioSnapshot;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.portfolio.repository.PortfolioSnapshotRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    private PortfolioSnapshotService portfolioSnapshotService;

    private Stock stock() {
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        ReflectionTestUtils.setField(stock, "id", 1L);
        return stock;
    }

    @DisplayName("당일 스냅샷이 없는 계좌는 새로 생성")
    @Test
    void 당일_스냅샷이_없는_계좌는_새로_생성() {
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
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
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
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

    @DisplayName("보유 종목이 있으면 현재가 기준 평가금액을 stockValue에 반영")
    @Test
    void 보유_종목이_있으면_현재가_기준_평가금액을_stockValue에_반영() {
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
        Account account = Account.builder()
                .userId(1L)
                .accountType(AccountType.BASIC)
                .build();
        account.addBalance(new BigDecimal("300000"));
        ReflectionTestUtils.setField(account, "id", 10L);
        Holding holding = Holding.builder()
                .accountId(10L)
                .stockId(1L)
                .quantity(10L)
                .averagePrice(new BigDecimal("60000"))
                .build();
        given(accountRepository.findAll()).willReturn(List.of(account));
        given(portfolioSnapshotRepository.existsByAccountIdAndSnapshotDate(10L, LocalDate.now())).willReturn(false);
        given(holdingRepository.findByAccountId(10L)).willReturn(List.of(holding));
        given(stockRepository.findAllById(any())).willReturn(List.of(stock()));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "2", "1.41", "88800", "49900"));

        portfolioSnapshotService.createDailySnapshots();

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(portfolioSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getStockValue()).isEqualByComparingTo("700000");
        assertThat(captor.getValue().getCash()).isEqualByComparingTo("300000");
        assertThat(captor.getValue().getTotalAsset()).isEqualByComparingTo("1000000");
    }

    @DisplayName("기간을 지정하면 해당 기간의 자산 추이 조회")
    @Test
    void 기간을_지정하면_해당_기간의_자산_추이_조회() {
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
        Account account = Account.builder()
                .userId(1L)
                .accountType(AccountType.BASIC)
                .build();
        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .accountId(10L)
                .snapshotDate(LocalDate.of(2026, 8, 20))
                .cash(new BigDecimal("300000"))
                .stockValue(new BigDecimal("700000"))
                .build();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 23);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(portfolioSnapshotRepository.findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(10L, from, to))
                .willReturn(List.of(snapshot));

        List<PortfolioSnapshotResponse> response = portfolioSnapshotService.getSnapshots(1L, 10L, from, to);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).totalAsset()).isEqualByComparingTo("1000000");
    }

    @DisplayName("기간을 지정하지 않으면 최근 1개월을 기본값으로 조회")
    @Test
    void 기간을_지정하지_않으면_최근_1개월을_기본값으로_조회() {
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
        Account account = Account.builder()
                .userId(1L)
                .accountType(AccountType.BASIC)
                .build();
        LocalDate today = LocalDate.now();
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(portfolioSnapshotRepository.findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                10L, today.minusMonths(1), today))
                .willReturn(List.of());

        List<PortfolioSnapshotResponse> response = portfolioSnapshotService.getSnapshots(1L, 10L, null, null);

        assertThat(response).isEmpty();
        verify(portfolioSnapshotRepository)
                .findByAccountIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(10L, today.minusMonths(1), today);
    }

    @DisplayName("다른 사용자의 계좌로 자산 추이를 조회할 시 예외 발생")
    @Test
    void 다른_사용자의_계좌로_자산_추이를_조회할_시_예외_발생() {
        portfolioSnapshotService = new PortfolioSnapshotService(
                accountRepository, portfolioSnapshotRepository, holdingRepository, stockRepository, kisQuoteClient);
        Account account = Account.builder()
                .userId(2L)
                .accountType(AccountType.BASIC)
                .build();
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> portfolioSnapshotService.getSnapshots(1L, 10L, null, null))
                .isInstanceOf(AccountException.class);
    }
}