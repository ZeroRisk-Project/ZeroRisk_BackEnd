package com.zerorisk.project.domain.order.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
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
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final AccountRepository accountRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;

    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        Account account = accountRepository.findByIdForUpdate(request.accountId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        Stock stock = stockRepository.findByCode(request.stockCode())
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new OrderException(OrderErrorCode.LIMIT_PRICE_REQUIRED);
        }

        Holding holding = holdingRepository.findByAccountIdAndStockId(account.getId(), stock.getId())
                .orElse(null);

        if (request.side() == OrderSide.SELL) {
            long ownedQuantity = holding == null ? 0 : holding.getQuantity();
            if (ownedQuantity < request.quantity()) {
                throw new OrderException(OrderErrorCode.INSUFFICIENT_HOLDING);
            }
        }

        BigDecimal currentPrice = fetchCurrentPrice(stock.getCode());
        boolean fillable = isFillable(request, currentPrice);
        BigDecimal executionPrice = request.orderType() == OrderType.MARKET ? currentPrice : request.limitPrice();

        if (request.side() == OrderSide.BUY) {
            BigDecimal cost = executionPrice.multiply(BigDecimal.valueOf(request.quantity()));
            if (account.getBalance().compareTo(cost) < 0) {
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