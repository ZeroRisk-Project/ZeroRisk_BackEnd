package com.zerorisk.project.domain.pricealert.service;

import com.zerorisk.project.domain.pricealert.dto.PriceAlertCreateRequest;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertResponse;
import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.repository.PriceAlertRepository;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
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
}