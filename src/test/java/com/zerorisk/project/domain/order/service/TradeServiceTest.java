package com.zerorisk.project.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.order.dto.TradeResponse;
import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.Trade;
import com.zerorisk.project.domain.order.repository.TradeRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    private TradeService tradeService;

    private Stock stock() {
        return Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
    }

    private Account account(Long userId) {
        return Account.builder()
                .userId(userId)
                .accountType(AccountType.BASIC)
                .build();
    }

    @DisplayName("계좌의 체결 내역을 페이징으로 조회")
    @Test
    void 계좌의_체결_내역을_페이징으로_조회() {
        tradeService = new TradeService(tradeRepository, accountRepository, stockRepository);
        Account account = account(1L);
        Trade trade = Trade.builder()
                .orderId(5L)
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .quantity(10L)
                .price(new BigDecimal("70000"))
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(tradeRepository.findByAccountId(10L, pageable)).willReturn(new PageImpl<>(List.of(trade)));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));

        Page<TradeResponse> response = tradeService.getTrades(1L, 10L, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).stockCode()).isEqualTo("005930");
    }

    @DisplayName("다른 사용자의 계좌로 체결 내역을 조회할 시 예외 발생")
    @Test
    void 다른_사용자의_계좌로_체결_내역을_조회할_시_예외_발생() {
        tradeService = new TradeService(tradeRepository, accountRepository, stockRepository);
        Account account = account(2L);
        Pageable pageable = PageRequest.of(0, 10);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> tradeService.getTrades(1L, 10L, pageable))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("존재하지 않는 계좌로 체결 내역을 조회할 시 예외 발생")
    @Test
    void 존재하지_않는_계좌로_체결_내역을_조회할_시_예외_발생() {
        tradeService = new TradeService(tradeRepository, accountRepository, stockRepository);
        Pageable pageable = PageRequest.of(0, 10);
        given(accountRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.getTrades(1L, 10L, pageable))
                .isInstanceOf(AccountException.class);
    }
}