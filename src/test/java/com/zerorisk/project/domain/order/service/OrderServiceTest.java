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
import com.zerorisk.project.domain.competition.repository.CompetitionAllowedStockRepository;
import com.zerorisk.project.domain.competition.repository.CompetitionParticipantRepository;
import com.zerorisk.project.domain.competition.service.CompetitionAssetService;
import com.zerorisk.project.domain.order.dto.OrderCreateRequest;
import com.zerorisk.project.domain.order.dto.OrderResponse;
import com.zerorisk.project.domain.order.dto.OrderSummaryResponse;
import com.zerorisk.project.domain.order.entity.Order;
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
import com.zerorisk.project.global.audit.UserActivityLogger;
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

    @Mock
    private CompetitionAllowedStockRepository competitionAllowedStockRepository;

    @Mock
    private CompetitionParticipantRepository competitionParticipantRepository;

    @Mock
    private CompetitionAssetService competitionAssetService;

    @Mock
    private UserActivityLogger userActivityLogger;

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
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
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
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
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
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
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
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
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
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(2L, new BigDecimal("1000000"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));

        OrderCreateRequest request = new OrderCreateRequest(10L, "005930", OrderSide.BUY, OrderType.MARKET, 10L, null);

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("미체결 주문을 취소하면 CANCELLED로 상태 전환")
    @Test
    void 미체결_주문을_취소하면_CANCELLED로_상태_전환() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(1L, BigDecimal.ZERO);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("60000"))
                .build();
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        orderService.cancelOrder(1L, 5L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("이미 체결된 주문을 취소할 시 예외 발생")
    @Test
    void 이미_체결된_주문을_취소할_시_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(1L, BigDecimal.ZERO);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.MARKET)
                .quantity(10L)
                .build();
        order.fill(new BigDecimal("70000"));
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ALREADY_PROCESSED);
    }

    @DisplayName("존재하지 않는 주문을 취소할 시 예외 발생")
    @Test
    void 존재하지_않는_주문을_취소할_시_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        given(orderRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(OrderException.class)
                .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.NOT_FOUND);
    }

    @DisplayName("다른 사용자의 주문을 취소할 시 예외 발생")
    @Test
    void 다른_사용자의_주문을_취소할_시_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(2L, BigDecimal.ZERO);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("60000"))
                .build();
        given(orderRepository.findById(5L)).willReturn(Optional.of(order));
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 5L))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("계좌의 주문 내역을 페이징으로 조회")
    @Test
    void 계좌의_주문_내역을_페이징으로_조회() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(1L, BigDecimal.ZERO);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.MARKET)
                .quantity(10L)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(orderRepository.findByAccountId(10L, pageable)).willReturn(new PageImpl<>(List.of(order)));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));

        Page<OrderSummaryResponse> response = orderService.getOrders(1L, 10L, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).stockCode()).isEqualTo("005930");
    }

    @DisplayName("상태 필터를 지정하면 해당 상태의 주문만 조회")
    @Test
    void 상태_필터를_지정하면_해당_상태의_주문만_조회() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(1L, BigDecimal.ZERO);
        Pageable pageable = PageRequest.of(0, 10);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(orderRepository.findByAccountIdAndStatus(10L, OrderStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of()));

        orderService.getOrders(1L, 10L, OrderStatus.PENDING, pageable);

        verify(orderRepository).findByAccountIdAndStatus(10L, OrderStatus.PENDING, pageable);
        verify(orderRepository, never()).findByAccountId(any(), any());
    }

    @DisplayName("다른 사용자의 계좌로 주문 내역을 조회할 시 예외 발생")
    @Test
    void 다른_사용자의_계좌로_주문_내역을_조회할_시_예외_발생() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Account account = account(2L, BigDecimal.ZERO);
        Pageable pageable = PageRequest.of(0, 10);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> orderService.getOrders(1L, 10L, null, pageable))
                .isInstanceOf(AccountException.class);
    }

    @DisplayName("지정가 매수 주문이 목표가에 도달하면 체결")
    @Test
    void 지정가_매수_주문이_목표가에_도달하면_체결() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("60000"))
                .build();
        Account account = account(1L, new BigDecimal("1000000"));
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(orderRepository.findByStatusAndOrderType(OrderStatus.PENDING, OrderType.LIMIT)).willReturn(List.of(order));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "55000", "1000", "5", "1.79", "88800", "49900"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(holdingRepository.findByAccountIdAndStockId(10L, 1L)).willReturn(Optional.empty());

        orderService.fillPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getFilledPrice()).isEqualByComparingTo("60000");
        assertThat(account.getBalance()).isEqualByComparingTo("400000");
    }

    @DisplayName("지정가 매도 주문이 목표가에 도달하지 못하면 대기 상태를 유지")
    @Test
    void 지정가_매도_주문이_목표가에_도달하지_못하면_대기_상태를_유지() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.SELL)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("80000"))
                .build();
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(orderRepository.findByStatusAndOrderType(OrderStatus.PENDING, OrderType.LIMIT)).willReturn(List.of(order));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "75000", "1000", "5", "1.32", "88800", "49900"));

        orderService.fillPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @DisplayName("잔액이 부족하면 목표가에 도달해도 매수 주문을 체결하지 않음")
    @Test
    void 잔액이_부족하면_목표가에_도달해도_매수_주문을_체결하지_않음() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("60000"))
                .build();
        Account account = account(1L, new BigDecimal("1000"));
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(orderRepository.findByStatusAndOrderType(OrderStatus.PENDING, OrderType.LIMIT)).willReturn(List.of(order));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "55000", "1000", "5", "1.79", "88800", "49900"));
        given(accountRepository.findByIdForUpdate(10L)).willReturn(Optional.of(account));
        given(holdingRepository.findByAccountIdAndStockId(10L, 1L)).willReturn(Optional.empty());

        orderService.fillPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(tradeRepository, never()).save(any());
    }

    @DisplayName("현재가 조회에 실패하면 해당 주문은 체결하지 않음")
    @Test
    void 현재가_조회에_실패하면_해당_주문은_체결하지_않음() {
        orderService = new OrderService(orderRepository, tradeRepository, holdingRepository, accountRepository, stockRepository, kisQuoteClient, competitionAllowedStockRepository, competitionParticipantRepository, competitionAssetService, userActivityLogger);
        Order order = Order.builder()
                .accountId(10L)
                .stockId(1L)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10L)
                .limitPrice(new BigDecimal("60000"))
                .build();
        Stock stock = stock();
        ReflectionTestUtils.setField(stock, "id", 1L);
        given(orderRepository.findByStatusAndOrderType(OrderStatus.PENDING, OrderType.LIMIT)).willReturn(List.of(order));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willThrow(new RuntimeException("KIS 조회 실패"));

        orderService.fillPendingOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }
}