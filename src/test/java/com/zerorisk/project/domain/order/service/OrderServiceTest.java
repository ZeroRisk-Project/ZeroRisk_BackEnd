package com.zerorisk.project.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.order.dto.OrderCreateRequest;
import com.zerorisk.project.domain.order.dto.OrderResponse;
import com.zerorisk.project.domain.order.entity.OrderSide;
import com.zerorisk.project.domain.order.entity.OrderStatus;
import com.zerorisk.project.domain.order.entity.OrderType;
import com.zerorisk.project.domain.order.exception.OrderErrorCode;
import com.zerorisk.project.domain.order.exception.OrderException;
import com.zerorisk.project.domain.order.repository.OrderRepository;
import com.zerorisk.project.domain.order.repository.TradeRepository;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    private OrderService orderService;

    private Stock stock() {
        return Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
    }

    private Account account(Long userId, BigDecimal balance) {
        Account account = Account.builder()
                .userId(userId)
                .accountType(AccountType.BASIC)
                .build();
        account.addBalance(balance);
        return account;
    }

    @DisplayName("시장가 매수는 즉시 체결되고 잔액 차감")
    @Test
    void 시장가_매수는_즉시_체결되고_잔액_차감() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(1L, new BigDecimal("1000000"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        given(holdingRepository.findByAccountIdAndStockId(any(), any())).willReturn(Optional.empty());
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "2", "1.41", "88800", "49900"));

        OrderCreateRequest request = new OrderCreateRequest(10L, "005930", OrderSide.BUY, OrderType.MARKET, 10L, null);
        OrderResponse response = orderService.createOrder(1L, request);

        assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(response.filledPrice()).isEqualByComparingTo("70000");
        assertThat(account.getBalance()).isEqualByComparingTo("300000");
        verify(tradeRepository).save(any());
        verify(holdingRepository).save(any());
    }

    @DisplayName("지정가 매수는 조건을 만족하지 못하면 대기 상태 유지")
    @Test
    void 지정가_매수는_조건을_만족하지_못하면_대기_상태_유지() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(1L, new BigDecimal("1000000"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        given(holdingRepository.findByAccountIdAndStockId(any(), any())).willReturn(Optional.empty());
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "2", "1.41", "88800", "49900"));

        OrderCreateRequest request = new OrderCreateRequest(
                10L, "005930", OrderSide.BUY, OrderType.LIMIT, 10L, new BigDecimal("60000"));
        OrderResponse response = orderService.createOrder(1L, request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(account.getBalance()).isEqualByComparingTo("1000000");
        verify(tradeRepository, never()).save(any());
    }

    @DisplayName("잔액이 부족하면 예외 발생")
    @Test
    void 잔액이_부족하면_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(1L, new BigDecimal("1000"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        lenient().when(holdingRepository.findByAccountIdAndStockId(any(), any())).thenReturn(Optional.empty());
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "2", "1.41", "88800", "49900"));

        OrderCreateRequest request = new OrderCreateRequest(10L, "005930", OrderSide.BUY, OrderType.MARKET, 10L, null);

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INSUFFICIENT_BALANCE);
    }

    @DisplayName("보유 수량보다 많은 매도를 시도하면 예외 발생")
    @Test
    void 보유_수량보다_많은_매도를_시도하면_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(1L, BigDecimal.ZERO);
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));
        given(holdingRepository.findByAccountIdAndStockId(any(), any())).willReturn(Optional.empty());

        OrderCreateRequest request = new OrderCreateRequest(10L, "005930", OrderSide.SELL, OrderType.MARKET, 10L, null);

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INSUFFICIENT_HOLDING);
    }

    @DisplayName("다른 사용자의 계좌로 주문하면 예외 발생")
    @Test
    void 다른_사용자의_계좌로_주문하면_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient);
        Account account = account(2L, new BigDecimal("1000000"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));

        OrderCreateRequest request = new OrderCreateRequest(10L, "005930", OrderSide.BUY, OrderType.MARKET, 10L, null);

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(AccountException.class);
    }
}