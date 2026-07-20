package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockQueryService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");

    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;

    @Transactional(readOnly = true)
    public StockDetailResponse getDetail(String code) {
        Stock stock = stockRepository.findByCode(code)
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        KisQuoteResponse.Output output = kisQuoteClient.fetchQuote(code);

        long currentPrice = Long.parseLong(output.currentPrice());
        long changeAmount = Long.parseLong(output.changeAmount());
        if (NEGATIVE_SIGNS.contains(output.changeSign())) {
            changeAmount = -changeAmount;
        }
        BigDecimal changeRate = new BigDecimal(output.changeRate());
        long week52High = Long.parseLong(output.week52High());
        long week52Low = Long.parseLong(output.week52Low());

        return new StockDetailResponse(
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                currentPrice,
                changeAmount,
                changeRate,
                week52High,
                week52Low);
    }
}