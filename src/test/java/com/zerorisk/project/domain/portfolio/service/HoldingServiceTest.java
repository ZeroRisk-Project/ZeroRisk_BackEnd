package com.zerorisk.project.domain.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.HoldingResponse;
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
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    private HoldingService holdingService;

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

    private Account account(Long userId) {
        return Account.builder()
                .userId(userId)
                .accountType(AccountType.BASIC)
                .build();
    }

    @DisplayName("보유 종목의 평가금액과 평가손익을 계산하여 조회")
    @Test
    void 보유_종목의_평가금액과_평가손익을_계산하여_조회() {
        holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(1L);
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

        List<HoldingResponse> response = holdingService.getHoldings(1L, 10L);

        assertThat(response).hasSize(1);
        HoldingResponse holdingResponse = response.get(0);
        assertThat(holdingResponse.stockCode()).isEqualTo("005930");
        assertThat(holdingResponse.evaluationAmount()).isEqualByComparingTo("700000");
        assertThat(holdingResponse.profitLoss()).isEqualByComparingTo("100000");
        assertThat(holdingResponse.profitRate()).isEqualByComparingTo("16.6700");
    }

    @DisplayName("다른 사용자의 계좌로 보유 종목을 조회할 시 예외 발생")
    @Test
    void 다른_사용자의_계좌로_보유_종목을_조회할_시_예외_발생() {
        holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(2L);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> holdingService.getHoldings(1L, 10L))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("존재하지 않는 계좌로 보유 종목을 조회할 시 예외 발생")
    @Test
    void 존재하지_않는_계좌로_보유_종목을_조회할_시_예외_발생() {
        holdingService = new HoldingService(holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        given(accountRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> holdingService.getHoldings(1L, 10L))
                .isInstanceOf(AccountException.class);
    }
}