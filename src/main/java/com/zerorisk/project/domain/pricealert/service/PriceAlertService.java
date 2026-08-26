package com.zerorisk.project.domain.pricealert.service;

import com.zerorisk.project.domain.notification.entity.NotificationType;
import com.zerorisk.project.domain.notification.service.NotificationService;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertCreateRequest;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertResponse;
import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.entity.PriceAlertDirection;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertErrorCode;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertException;
import com.zerorisk.project.domain.pricealert.repository.PriceAlertRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private static final long KIS_QUOTE_REQUEST_INTERVAL_MILLIS = 600;

    private final PriceAlertRepository priceAlertRepository;
    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;
    private final NotificationService notificationService;

    @Transactional
    public PriceAlertResponse createAlert(Long userId, PriceAlertCreateRequest request) {
        Stock stock = stockRepository.findByCode(request.stockCode())
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        PriceAlert alert = PriceAlert.builder()
                .userId(userId)
                .stockId(stock.getId())
                .targetPrice(request.targetPrice())
                .direction(request.direction())
                .build();
        priceAlertRepository.save(alert);

        return PriceAlertResponse.of(alert, stock);
    }

    @Transactional(readOnly = true)
    public List<PriceAlertResponse> getAlerts(Long userId) {
        List<PriceAlert> alerts = priceAlertRepository.findByUserId(userId);

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        alerts.stream().map(PriceAlert::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return alerts.stream()
                .map(alert -> PriceAlertResponse.of(alert, stocksById.get(alert.getStockId())))
                .toList();
    }

    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new PriceAlertException(PriceAlertErrorCode.NOT_FOUND));

        if (!alert.getUserId().equals(userId)) {
            throw new PriceAlertException(PriceAlertErrorCode.ACCESS_DENIED);
        }

        priceAlertRepository.delete(alert);
    }

    @Transactional
    public void dispatchAlerts() {
        List<PriceAlert> alerts = priceAlertRepository.findAll();
        if (alerts.isEmpty()) {
            return;
        }

        Map<Long, Stock> stocksById = stockRepository.findAllById(
                        alerts.stream().map(PriceAlert::getStockId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        Map<Long, BigDecimal> priceByStockId = fetchCurrentPrices(stocksById.values());

        int dispatched = 0;
        for (PriceAlert alert : alerts) {
            Stock stock = stocksById.get(alert.getStockId());
            BigDecimal currentPrice = priceByStockId.get(alert.getStockId());
            if (stock == null || currentPrice == null || !isTriggered(alert, currentPrice)) {
                continue;
            }

            notificationService.createNotification(
                    alert.getUserId(),
                    NotificationType.PRICE_ALERT,
                    "목표가 도달 알림",
                    stock.getName() + " 종목이 목표가 " + alert.getTargetPrice() + "원에 도달했습니다.",
                    "/stocks/" + stock.getCode());
            priceAlertRepository.delete(alert);
            dispatched++;
        }
        log.info("목표가 알림 발송 완료: {}건", dispatched);
    }

    private boolean isTriggered(PriceAlert alert, BigDecimal currentPrice) {
        return alert.getDirection() == PriceAlertDirection.ABOVE
                ? currentPrice.compareTo(alert.getTargetPrice()) >= 0
                : currentPrice.compareTo(alert.getTargetPrice()) <= 0;
    }

    private Map<Long, BigDecimal> fetchCurrentPrices(Collection<Stock> stocks) {
        Map<Long, BigDecimal> priceByStockId = new HashMap<>();
        for (Stock stock : stocks) {
            try {
                priceByStockId.put(stock.getId(), new BigDecimal(kisQuoteClient.fetchQuote(stock.getCode()).currentPrice()));
            } catch (Exception e) {
                log.warn("종목 {} 현재가 조회에 실패하여 이번 배치에서는 알림 판단을 건너뜁니다.", stock.getCode(), e);
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
}