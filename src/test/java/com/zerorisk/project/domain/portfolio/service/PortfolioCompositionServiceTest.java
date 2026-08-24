package com.zerorisk.project.domain.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioCompositionResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortfolioCompositionServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    private PortfolioCompositionService portfolioCompositionService;

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

    private Account account(Long userId, BigDecimal balance) {
        Account account = Account.builder()
                .userId(userId)
                .accountType(AccountType.BASIC)
                .build();
        account.addBalance(balance);
        return account;
    }

    @DisplayName("현금과 보유 종목 평가금액을 기준으로 자산 구성 비율 계산")
    @Test
    void 현금과_보유_종목_평가금액을_기준으로_자산_구성_비율_계산() {
        HoldingService holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        portfolioCompositionService = new PortfolioCompositionService(holdingService, accountRepository);
        Account account = account(1L, new BigDecimal("300000"));
        Holding holding = Holding.builder()
                .accountId(10L)
                .stockId(1L)
                .quantity(10L)
                .averagePrice(new BigDecimal("60000"))
                .build();
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(holdingRepository.findByAccountId(10L)).willReturn(List.of(holding));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "2", "1.41", "88800", "49900"));

        PortfolioCompositionResponse response = portfolioCompositionService.getComposition(1L, 10L);

        assertThat(response.cash()).isEqualByComparingTo("300000");
        assertThat(response.stockValue()).isEqualByComparingTo("700000");
        assertThat(response.totalAsset()).isEqualByComparingTo("1000000");
        assertThat(response.cashRatio()).isEqualByComparingTo("30.0000");
        assertThat(response.stockRatio()).isEqualByComparingTo("70.0000");
        assertThat(response.stocks()).hasSize(1);
        assertThat(response.stocks().get(0).weight()).isEqualByComparingTo("70.0000");
    }

    @DisplayName("다른 사용자의 계좌로 자산 구성을 조회 시 예외 발생")
    @Test
    void 다른_사용자의_계좌로_자산_구성을_조회_시_예외_발생() {
        HoldingService holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        portfolioCompositionService = new PortfolioCompositionService(holdingService, accountRepository);
        Account account = account(2L, BigDecimal.ZERO);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> portfolioCompositionService.getComposition(1L, 10L))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("보유 종목이 없으면 현금 비중을 100%로 계산")
    @Test
    void 보유_종목이_없으면_현금_비중을_100퍼센트로_계산() {
        HoldingService holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        portfolioCompositionService = new PortfolioCompositionService(holdingService, accountRepository);
        Account account = account(1L, new BigDecimal("500000"));
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(holdingRepository.findByAccountId(10L)).willReturn(List.of());

        PortfolioCompositionResponse response = portfolioCompositionService.getComposition(1L, 10L);

        assertThat(response.cashRatio()).isEqualByComparingTo("100.0000");
        assertThat(response.stockRatio()).isEqualByComparingTo("0.0000");
        assertThat(response.stocks()).isEmpty();
    }
}