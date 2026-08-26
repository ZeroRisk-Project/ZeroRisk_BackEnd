package com.zerorisk.project.domain.pricealert.service;

import com.zerorisk.project.domain.pricealert.dto.PriceAlertCreateRequest;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertResponse;
import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertErrorCode;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertException;
import com.zerorisk.project.domain.pricealert.repository.PriceAlertRepository;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final StockRepository stockRepository;

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
}