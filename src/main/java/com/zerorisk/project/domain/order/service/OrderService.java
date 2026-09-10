package com.zerorisk.project.domain.order.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.entity.AccountType;
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
import com.zerorisk.project.domain.order.entity.Trade;
import com.zerorisk.project.domain.order.exception.OrderErrorCode;
import com.zerorisk.project.domain.order.exception.OrderException;
import com.zerorisk.project.domain.order.repository.OrderRepository;
import com.zerorisk.project.domain.order.repository.TradeRepository;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final long KIS_QUOTE_REQUEST_INTERVAL_MILLIS = 600;

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;
    private final CompetitionAllowedStockRepository competitionAllowedStockRepository;
    private final CompetitionParticipantRepository competitionParticipantRepository;
    private final CompetitionAssetService competitionAssetService;
    private final UserActivityLogger userActivityLogger;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        Account account = accountRepository.findByIdForUpdate(request.accountId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        if (!account.isActive()) {
            throw new OrderException(OrderErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        Stock stock = stockRepository.findByCode(request.stockCode())
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        if (account.getAccountType() == AccountType.COMPETITION) {
            boolean hasRestriction = competitionAllowedStockRepository.existsByCompetitionId(account.getCompetitionId());
            if (hasRestriction && !competitionAllowedStockRepository.existsByCompetitionIdAndStockId(account.getCompetitionId(), stock.getId())) {
                throw new OrderException(OrderErrorCode.STOCK_NOT_ALLOWED_IN_COMPETITION);
            }
        }

        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new OrderException(OrderErrorCode.LIMIT_PRICE_REQUIRED);
        }

        Holding holding = holdingRepository.findByAccountIdAndStockId(account.getId(), stock.getId())
                .orElse(null);

        if (request.side() == OrderSide.SELL) {
            long ownedQuantity = holding == null ? 0 : holding.getQuantity();
            // 이미 걸려있는 다른 PENDING 지정가 매도 주문들이 약속한 수량은 또 팔 수 없다 -
            // 이걸 안 빼면 같은 주식을 두 번 팔겠다고 접수해버려서, 나중에 하나는 조용히 영원히
            // 체결 안 되는 상태로 남는다.
            long reservedQuantity = orderRepository.sumPendingSellReservedQuantity(account.getId(), stock.getId()).longValue();
            if (ownedQuantity - reservedQuantity < request.quantity()) {
                throw new OrderException(OrderErrorCode.INSUFFICIENT_HOLDING);
            }
        }

        BigDecimal currentPrice = fetchCurrentPrice(stock.getCode());
        boolean fillable = isFillable(request, currentPrice);
        BigDecimal executionPrice = request.orderType() == OrderType.MARKET ? currentPrice : request.limitPrice();

        if (request.side() == OrderSide.BUY) {
            BigDecimal cost = executionPrice.multiply(BigDecimal.valueOf(request.quantity()));
            // 이미 걸려있는 다른 PENDING 지정가 매수 주문들이 약속한 금액도 뺀 "실제 가용 잔고"로 판단한다 -
            // 안 그러면 잔고 10,000원으로 10,000원짜리 지정가 매수를 두 번 접수받을 수 있다.
            BigDecimal reservedAmount = orderRepository.sumPendingBuyReservedAmount(account.getId());
            BigDecimal availableBalance = account.getBalance().subtract(reservedAmount);
            if (availableBalance.compareTo(cost) < 0) {
                throw new OrderException(OrderErrorCode.INSUFFICIENT_BALANCE);
            }
        }

        Order order = Order.builder()
                .accountId(account.getId())
                .stockId(stock.getId())
                .side(request.side())
                .orderType(request.orderType())
                .quantity(request.quantity())
                .limitPrice(request.limitPrice())
                .build();
        orderRepository.save(order);

        if (fillable) {
            executeFill(order, account, holding, executionPrice);
        }

        String actionType = request.side() == OrderSide.BUY ? "ORDER_BUY" : "ORDER_SELL";
        userActivityLogger.log(userId, actionType,
                String.format("%s %d주 %s (%s)", stock.getName(), request.quantity(),
                        fillable ? "체결" : "접수(대기)", request.orderType()));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(Long userId, Long accountId, OrderStatus status, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        Page<Order> orders = status == null
                ? orderRepository.findByAccountId(accountId, pageable)
                : orderRepository.findByAccountIdAndStatus(accountId, status, pageable);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        orders.getContent().stream().map(Order::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return orders.map(order -> OrderSummaryResponse.of(order, stocksById.get(order.getStockId())));
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.NOT_FOUND));

        Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        if (!order.isPending()) {
            throw new OrderException(OrderErrorCode.ALREADY_PROCESSED);
        }

        order.cancel();
        userActivityLogger.log(userId, "ORDER_CANCEL", "주문 취소 (주문번호 " + orderId + ")");
    }

    // 회원 탈퇴 전 체크용 - 이 유저의 모든 계좌(BASIC/COMPETITION)에 걸린 미체결 주문 목록을 반환한다.
    // 비어있지 않으면 UserService.withdraw()가 이 목록을 실어 탈퇴를 막는다 (유저가 직접 정리하도록 유도).
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getPendingOrders(Long userId) {
        List<Long> accountIds = accountRepository.findByUserId(userId).stream()
                .map(Account::getId)
                .toList();

        if (accountIds.isEmpty()) {
            return List.of();
        }

        List<Order> pendingOrders = orderRepository.findByAccountIdInAndStatus(accountIds, OrderStatus.PENDING);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        pendingOrders.stream().map(Order::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return pendingOrders.stream()
                .map(order -> OrderSummaryResponse.of(order, stocksById.get(order.getStockId())))
                .toList();
    }

    @Transactional
    public void fillPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatusAndOrderType(OrderStatus.PENDING, OrderType.LIMIT);
        if (pendingOrders.isEmpty()) {
            return;
        }

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        pendingOrders.stream().map(Order::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        Map<Long, BigDecimal> priceByStockId = fetchCurrentPrices(stocksById.values());

        int filled = 0;
        for (Order order : pendingOrders) {
            BigDecimal currentPrice = priceByStockId.get(order.getStockId());
            if (currentPrice == null || !isFillable(order, currentPrice)) {
                continue;
            }
            if (tryFillPendingOrder(order)) {
                filled++;
            }
        }
        log.info("예약 주문 체결 배치 완료: {}건", filled);
    }

    private boolean tryFillPendingOrder(Order staleOrder) {
        // fillPendingOrders() 최초 조회는 락 없는 스냅샷이라, KIS 시세 조회로 지연되는 동안
        // 사용자가 이 주문을 취소했을 수 있다. 실제 체결 직전에 락을 걸고 상태를 다시 확인해서,
        // 이미 취소된 주문을 체결해버리는 레이스(cancelOrder도 findById에 같은 락을 쓴다)를 막는다.
        Order order = orderRepository.findById(staleOrder.getId())
                .orElse(null);
        if (order == null || !order.isPending()) {
            return false;
        }

        Account account = accountRepository.findByIdForUpdate(order.getAccountId())
                .orElse(null);
        if (account == null) {
            return false;
        }

        Holding holding = holdingRepository.findByAccountIdAndStockId(order.getAccountId(), order.getStockId())
                .orElse(null);
        BigDecimal executionPrice = order.getLimitPrice();
        BigDecimal amount = executionPrice.multiply(BigDecimal.valueOf(order.getQuantity()));

        if (order.getSide() == OrderSide.BUY && account.getBalance().compareTo(amount) < 0) {
            return false;
        }
        if (order.getSide() == OrderSide.SELL) {
            long ownedQuantity = holding == null ? 0 : holding.getQuantity();
            if (ownedQuantity < order.getQuantity()) {
                return false;
            }
        }

        executeFill(order, account, holding, executionPrice);
        return true;
    }

    private boolean isFillable(Order order, BigDecimal currentPrice) {
        return order.getSide() == OrderSide.BUY
                ? currentPrice.compareTo(order.getLimitPrice()) <= 0
                : currentPrice.compareTo(order.getLimitPrice()) >= 0;
    }

    private Map<Long, BigDecimal> fetchCurrentPrices(Collection<Stock> stocks) {
        Map<Long, BigDecimal> priceByStockId = new HashMap<>();
        for (Stock stock : stocks) {
            try {
                priceByStockId.put(stock.getId(), fetchCurrentPrice(stock.getCode()));
            } catch (Exception e) {
                log.warn("종목 {} 현재가 조회에 실패하여 이번 배치에서는 체결 판단을 건너뜁니다.", stock.getCode(), e);
            }
            sleepForThrottle();
        }
        return priceByStockId;
    }

    private void sleepForThrottle() {
        try {
            Thread.sleep(KIS_QUOTE_REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void executeFill(Order order, Account account, Holding holding, BigDecimal executionPrice) {
        order.fill(executionPrice);

        tradeRepository.save(Trade.builder()
                .orderId(order.getId())
                .accountId(order.getAccountId())
                .stockId(order.getStockId())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .price(executionPrice)
                .build());

        BigDecimal amount = executionPrice.multiply(BigDecimal.valueOf(order.getQuantity()));

        if (order.getSide() == OrderSide.BUY) {
            account.addBalance(amount.negate());
            applyBuyToHolding(order, holding, executionPrice);
        } else {
            account.addBalance(amount);
            applySellToHolding(holding, order.getQuantity());
        }

        if (account.getAccountType() == AccountType.COMPETITION) {
            competitionParticipantRepository.findByAccountId(account.getId())
                    .ifPresent(competitionAssetService::recalculate);
        }
    }

    private void applyBuyToHolding(Order order, Holding holding, BigDecimal executionPrice) {
        if (holding == null) {
            holdingRepository.save(Holding.builder()
                    .accountId(order.getAccountId())
                    .stockId(order.getStockId())
                    .quantity(order.getQuantity())
                    .averagePrice(executionPrice)
                    .build());
        } else {
            holding.applyBuy(order.getQuantity(), executionPrice);
        }
    }

    private void applySellToHolding(Holding holding, Long quantity) {
        holding.applySell(quantity);
        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        }
    }

    private boolean isFillable(OrderCreateRequest request, BigDecimal currentPrice) {
        if (request.orderType() == OrderType.MARKET) {
            return true;
        }
        return request.side() == OrderSide.BUY
                ? currentPrice.compareTo(request.limitPrice()) <= 0
                : currentPrice.compareTo(request.limitPrice()) >= 0;
    }

    private BigDecimal fetchCurrentPrice(String code) {
        return new BigDecimal(kisQuoteClient.fetchQuote(code).currentPrice());
    }
}