package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisOrderBookClient;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisOrderBookResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.dto.OrderBookLevel;
import com.zerorisk.project.domain.stock.dto.OrderBookResponse;
import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import com.zerorisk.project.global.exception.StockQuoteUnavailableException;
import java.math.BigDecimal;
import java.util.List;
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
    private final KisOrderBookClient kisOrderBookClient;

    @Transactional(readOnly = true)
    public StockDetailResponse getDetail(String code) {
        Stock stock = stockRepository.findByCode(code)
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        KisQuoteResponse.Output output;
        try {
            output = kisQuoteClient.fetchQuote(code);
        } catch (Exception e) {
            throw new StockQuoteUnavailableException(e);
        }

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

    @Transactional(readOnly = true)
    public OrderBookResponse getOrderBook(String code) {
        stockRepository.findByCode(code)
                .filter(Stock::getActive)
                .orElseThrow(StockNotFoundException::new);

        KisOrderBookResponse.Output1 output;
        try {
            output = kisOrderBookClient.fetchOrderBook(code);
        } catch (Exception e) {
            throw new StockQuoteUnavailableException("호가 조회에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }

        // sellLevels는 10단계(가장 먼 호가)부터 1단계(가장 가까운 호가) 순으로 담아, 화면에 위에서
        // 아래로 그리면 현재가에 가까운 호가가 중앙(현재가 표시줄) 바로 위에 오도록 한다.
        List<OrderBookLevel> sellLevels = List.of(
                new OrderBookLevel(Long.parseLong(output.askPrice10()), Long.parseLong(output.askQuantity10())),
                new OrderBookLevel(Long.parseLong(output.askPrice9()), Long.parseLong(output.askQuantity9())),
                new OrderBookLevel(Long.parseLong(output.askPrice8()), Long.parseLong(output.askQuantity8())),
                new OrderBookLevel(Long.parseLong(output.askPrice7()), Long.parseLong(output.askQuantity7())),
                new OrderBookLevel(Long.parseLong(output.askPrice6()), Long.parseLong(output.askQuantity6())),
                new OrderBookLevel(Long.parseLong(output.askPrice5()), Long.parseLong(output.askQuantity5())),
                new OrderBookLevel(Long.parseLong(output.askPrice4()), Long.parseLong(output.askQuantity4())),
                new OrderBookLevel(Long.parseLong(output.askPrice3()), Long.parseLong(output.askQuantity3())),
                new OrderBookLevel(Long.parseLong(output.askPrice2()), Long.parseLong(output.askQuantity2())),
                new OrderBookLevel(Long.parseLong(output.askPrice1()), Long.parseLong(output.askQuantity1())));

        // buyLevels는 1단계(가장 가까운 호가)부터 10단계(가장 먼 호가) 순 - 현재가 표시줄 바로
        // 아래부터 자연스럽게 이어지도록 한다.
        List<OrderBookLevel> buyLevels = List.of(
                new OrderBookLevel(Long.parseLong(output.bidPrice1()), Long.parseLong(output.bidQuantity1())),
                new OrderBookLevel(Long.parseLong(output.bidPrice2()), Long.parseLong(output.bidQuantity2())),
                new OrderBookLevel(Long.parseLong(output.bidPrice3()), Long.parseLong(output.bidQuantity3())),
                new OrderBookLevel(Long.parseLong(output.bidPrice4()), Long.parseLong(output.bidQuantity4())),
                new OrderBookLevel(Long.parseLong(output.bidPrice5()), Long.parseLong(output.bidQuantity5())),
                new OrderBookLevel(Long.parseLong(output.bidPrice6()), Long.parseLong(output.bidQuantity6())),
                new OrderBookLevel(Long.parseLong(output.bidPrice7()), Long.parseLong(output.bidQuantity7())),
                new OrderBookLevel(Long.parseLong(output.bidPrice8()), Long.parseLong(output.bidQuantity8())),
                new OrderBookLevel(Long.parseLong(output.bidPrice9()), Long.parseLong(output.bidQuantity9())),
                new OrderBookLevel(Long.parseLong(output.bidPrice10()), Long.parseLong(output.bidQuantity10())));

        return new OrderBookResponse(
                sellLevels,
                buyLevels,
                Long.parseLong(output.totalAskQuantity()),
                Long.parseLong(output.totalBidQuantity()));
    }
}