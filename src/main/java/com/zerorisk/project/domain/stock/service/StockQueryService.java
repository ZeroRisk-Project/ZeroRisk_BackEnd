package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisOrderBookClient;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisOrderBookResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.dto.OrderBookLevel;
import com.zerorisk.project.domain.stock.dto.OrderBookResponse;
import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.dto.StockQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import com.zerorisk.project.global.exception.StockQuoteUnavailableException;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockQueryService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");

    // "전체보기"/검색 화면 한 페이지에 보이는 종목 수를 넘지 않도록 막아, 코드 목록을 잔뜩
    // 넘겨 KIS 시세 조회를 과도하게 호출하는 것을 방지한다.
    private static final int MAX_QUOTE_CODES = 30;

    // 종목마다 순차 조회하면 페이지 하나(최대 30종목) 로딩에 수 초가 걸려 체감 속도가
    // 크게 떨어지므로, KIS 호출 제한에 걸리지 않는 선에서 동시에 몇 개씩 병렬 조회한다.
    private static final int QUOTE_CONCURRENCY = 5;

    private final StockRepository stockRepository;
    private final KisQuoteClient kisQuoteClient;
    private final KisOrderBookClient kisOrderBookClient;
    private final ExecutorService quoteExecutor = Executors.newFixedThreadPool(QUOTE_CONCURRENCY);

    @PreDestroy
    void shutdownQuoteExecutor() {
        quoteExecutor.shutdown();
    }

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

        long week52High = Long.parseLong(output.week52High());
        long week52Low = Long.parseLong(output.week52Low());

        return new StockDetailResponse(
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                Long.parseLong(output.currentPrice()),
                parseChangeAmount(output),
                parseChangeRate(output),
                week52High,
                week52Low);
    }

    // 화면에 실제로 보이는 종목만큼만 개별 시세를 조회한다(페이지당 최대 MAX_QUOTE_CODES개).
    // QUOTE_CONCURRENCY만큼 동시에 조회해 순차 조회 대비 응답 시간을 크게 줄이고, 조회에
    // 실패한 종목은 건너뛰고 나머지 결과만 반환한다.
    public List<StockQuoteResponse> getQuotes(List<String> codes) {
        List<CompletableFuture<StockQuoteResponse>> futures = codes.stream()
                .distinct()
                .limit(MAX_QUOTE_CODES)
                .map(code -> CompletableFuture.supplyAsync(() -> fetchQuoteOrNull(code), quoteExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    private StockQuoteResponse fetchQuoteOrNull(String code) {
        try {
            KisQuoteResponse.Output output = kisQuoteClient.fetchQuote(code);
            return new StockQuoteResponse(
                    code,
                    Long.parseLong(output.currentPrice()),
                    parseChangeAmount(output),
                    parseChangeRate(output));
        } catch (Exception e) {
            log.warn("시세 조회 실패 - 목록에서 제외: code={}", code, e);
            return null;
        }
    }

    // KIS가 changeAmount/changeRate 문자열에 부호를 이미 포함해 내려주는 경우와 그렇지 않은
    // 경우가 섞여 있어, 항상 abs()로 정규화한 뒤 changeSign(전일대비부호)만을 유일한 근거로
    // 부호를 다시 매겨 두 값의 부호가 항상 일치하도록 한다.
    private long parseChangeAmount(KisQuoteResponse.Output output) {
        long changeAmount = Math.abs(Long.parseLong(output.changeAmount()));
        return NEGATIVE_SIGNS.contains(output.changeSign()) ? -changeAmount : changeAmount;
    }

    private BigDecimal parseChangeRate(KisQuoteResponse.Output output) {
        BigDecimal changeRate = new BigDecimal(output.changeRate()).abs();
        return NEGATIVE_SIGNS.contains(output.changeSign()) ? changeRate.negate() : changeRate;
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