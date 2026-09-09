package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.KisRankingClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import com.zerorisk.project.domain.stock.dto.RankingType;
import com.zerorisk.project.domain.stock.dto.StockRankingResponse;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockCodeType;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository.StockFavoriteCount;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRankingService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");

    // KIS 거래량순위(FHPST01710000)는 한 번 호출에 시장 구분당 상위 30건만 내려주고
    // 페이징 파라미터가 없다. "0000"(전체)만으로는 두 시장을 합친 상위 30건만 보이므로,
    // 코스닥(1001)을 별도로 한 번 더 호출해 코스닥 상위권이 전체 랭킹에 가려지지 않게 합친다.
    // ("0001" 등 코스피 전용 코드는 이 TR에서 500을 반환해 지원되지 않는 것으로 확인됨.)
    private static final List<String> MARKET_CODES = List.of("0000", "1001");

    // 인기 랭킹은 종목마다 실시간 시세를 별도로 조회해야 해서, 관심종목 상위 종목이 많아도
    // KIS 호출이 과도해지지 않도록 상한을 둔다.
    private static final int POPULAR_QUOTE_LIMIT = 30;

    private final KisRankingClient kisRankingClient;
    private final KisQuoteClient kisQuoteClient;
    private final WatchlistFavoriteRepository watchlistFavoriteRepository;
    private final StockRepository stockRepository;

    public List<StockRankingResponse> getRankings(RankingType type, int count) {
        if (type == RankingType.POPULAR) {
            return getPopularRankings(Math.min(count, POPULAR_QUOTE_LIMIT));
        }

        Map<String, StockRankingResponse> byCode = new LinkedHashMap<>();
        for (String marketCode : MARKET_CODES) {
            try {
                for (KisRankingResponse.Output output : kisRankingClient.fetchVolumeRanking(marketCode)) {
                    StockRankingResponse response = toResponse(output);
                    byCode.putIfAbsent(response.code(), response);
                }
            } catch (Exception e) {
                log.warn("거래량순위 조회 실패: marketCode={}", marketCode, e);
            }
        }
        List<StockRankingResponse> rankings = List.copyOf(byCode.values());

        Comparator<StockRankingResponse> comparator = switch (type) {
            case VOLUME -> Comparator.comparing(StockRankingResponse::volume).reversed();
            case RISE -> Comparator.comparing(StockRankingResponse::changeRate).reversed();
            case FALL -> Comparator.comparing(StockRankingResponse::changeRate);
            case TRADING_VALUE -> Comparator
                    .comparing((StockRankingResponse r) -> r.currentPrice() * r.volume())
                    .reversed();
            case POPULAR -> throw new IllegalStateException("POPULAR은 별도 경로로 처리됩니다.");
        };

        return rankings.stream()
                .sorted(comparator)
                .limit(count)
                .toList();
    }

    private List<StockRankingResponse> getPopularRankings(int count) {
        List<StockFavoriteCount> favoriteCounts =
                watchlistFavoriteRepository.countGroupedByStockDesc(PageRequest.of(0, count));
        if (favoriteCounts.isEmpty()) {
            return List.of();
        }

        List<Long> stockIds = favoriteCounts.stream().map(StockFavoriteCount::getStockId).toList();
        Map<Long, Stock> stocksById = stockRepository.findAllById(stockIds).stream()
                .collect(Collectors.toMap(Stock::getId, stock -> stock));

        List<StockRankingResponse> rankings = new ArrayList<>();
        for (StockFavoriteCount favoriteCount : favoriteCounts) {
            Stock stock = stocksById.get(favoriteCount.getStockId());
            if (stock == null || !stock.getActive()) {
                continue;
            }
            try {
                KisQuoteResponse.Output output = kisQuoteClient.fetchQuote(stock.getCode());
                rankings.add(toResponse(stock, output));
            } catch (Exception e) {
                log.warn("인기 종목 시세 조회 실패: code={}", stock.getCode(), e);
            }
        }
        return rankings;
    }

    private StockRankingResponse toResponse(KisRankingResponse.Output output) {
        long changeAmount = Long.parseLong(output.changeAmount());
        if (NEGATIVE_SIGNS.contains(output.changeSign())) {
            changeAmount = -changeAmount;
        }

        return new StockRankingResponse(
                output.code(),
                output.name(),
                Long.parseLong(output.currentPrice()),
                changeAmount,
                new BigDecimal(output.changeRate()),
                Long.parseLong(output.volume()),
                StockCodeType.isPreferred(output.code()));
    }

    private StockRankingResponse toResponse(Stock stock, KisQuoteResponse.Output output) {
        long changeAmount = Long.parseLong(output.changeAmount());
        if (NEGATIVE_SIGNS.contains(output.changeSign())) {
            changeAmount = -changeAmount;
        }

        return new StockRankingResponse(
                stock.getCode(),
                stock.getName(),
                Long.parseLong(output.currentPrice()),
                changeAmount,
                new BigDecimal(output.changeRate()),
                0L,
                StockCodeType.isPreferred(stock.getCode()));
    }
}
