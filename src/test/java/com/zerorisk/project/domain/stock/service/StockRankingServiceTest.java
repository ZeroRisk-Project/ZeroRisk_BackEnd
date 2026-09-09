package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.KisRankingClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import com.zerorisk.project.domain.stock.dto.RankingType;
import com.zerorisk.project.domain.stock.dto.StockRankingResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository;
import com.zerorisk.project.domain.watchlist.repository.WatchlistFavoriteRepository.StockFavoriteCount;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockRankingServiceTest {

    @Mock
    private KisRankingClient kisRankingClient;

    @Mock
    private KisQuoteClient kisQuoteClient;

    @Mock
    private WatchlistFavoriteRepository watchlistFavoriteRepository;

    @Mock
    private StockRepository stockRepository;

    private StockRankingService stockRankingService;

    private StockRankingService service() {
        return new StockRankingService(kisRankingClient, kisQuoteClient, watchlistFavoriteRepository, stockRepository);
    }

    @DisplayName("RISE 조회 시 등락률 내림차순 정렬")
    @Test
    void RISE_조회_시_등락률_내림차순_정렬() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "500"),
                new KisRankingResponse.Output("000002", "B", "20000", "600", "2", "3.00", "300"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "900")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.RISE, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000002", "000001", "000003");
    }

    @DisplayName("FALL 조회 시 등락률 오름차순 정렬")
    @Test
    void FALL_조회_시_등락률_오름차순_정렬() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "500"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "900")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.FALL, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000003", "000001");
        assertThat(result.get(0).changeAmount()).isEqualTo(-200L);
    }

    @DisplayName("count로 상위 N건 제한")
    @Test
    void count로_상위_N건_제한() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900"),
                new KisRankingResponse.Output("000002", "B", "20000", "600", "2", "3.00", "500"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "300")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 2);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000001", "000002");
    }

    @DisplayName("전체/코스닥 두 시장 조회 결과를 합치고 중복 종목은 한 번만 남긴다")
    @Test
    void 두_시장_조회_결과를_합치고_중복은_제거한다() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(eq("0000"))).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900")));
        given(kisRankingClient.fetchVolumeRanking(eq("1001"))).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900"),
                new KisRankingResponse.Output("000099", "코스닥전용", "5000", "50", "2", "1.00", "700")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactlyInAnyOrder("000001", "000099");
    }

    @DisplayName("한 시장 조회가 실패해도 다른 시장 결과는 반환한다")
    @Test
    void 한_시장_조회_실패해도_나머지_결과는_반환한다() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(eq("0000")))
                .willThrow(new IllegalStateException("KIS 오류"));
        given(kisRankingClient.fetchVolumeRanking(eq("1001"))).willReturn(List.of(
                new KisRankingResponse.Output("000099", "코스닥전용", "5000", "50", "2", "1.00", "700")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000099");
    }

    @DisplayName("코드 마지막 자리로 보통주/우선주를 구분해 반환한다")
    @Test
    void 코드_마지막_자리로_보통주_우선주를_구분한다() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("005930", "삼성전자", "70000", "100", "2", "1.00", "900"),
                new KisRankingResponse.Output("005935", "삼성전자우", "68000", "100", "2", "1.00", "300")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 10);

        assertThat(result).extracting(StockRankingResponse::code, StockRankingResponse::preferred)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("005930", false),
                        Tuple.tuple("005935", true));
    }

    @DisplayName("TRADING_VALUE 조회 시 가격x거래량 내림차순 정렬")
    @Test
    void TRADING_VALUE_조회_시_가격곱거래량_내림차순_정렬() {
        stockRankingService = service();
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "1000", "0", "2", "0.00", "100000"),
                new KisRankingResponse.Output("000002", "B", "100000", "0", "2", "0.00", "5000")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.TRADING_VALUE, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000002", "000001");
    }

    @DisplayName("POPULAR 조회 시 관심종목 등록 수 상위 종목의 실시간 시세를 채워 반환한다")
    @Test
    void POPULAR_조회_시_관심종목_등록_수_상위_종목_시세를_채운다() {
        stockRankingService = service();

        StockFavoriteCount favoriteCount = mockFavoriteCount(1L, 5L);
        given(watchlistFavoriteRepository.countGroupedByStockDesc(any())).willReturn(List.of(favoriteCount));

        Stock stock = stockWithId(1L, "005930", "삼성전자");
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock));
        given(kisQuoteClient.fetchQuote("005930"))
                .willReturn(new KisQuoteResponse.Output("70000", "1000", "2", "1.45", "80000", "60000"));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.POPULAR, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("005930");
        assertThat(result.get(0).currentPrice()).isEqualTo(70000L);
    }

    @DisplayName("POPULAR 조회 중 일부 종목 시세 조회가 실패해도 나머지는 반환한다")
    @Test
    void POPULAR_조회_중_일부_시세_실패해도_나머지는_반환한다() {
        stockRankingService = service();

        StockFavoriteCount favorite1 = mockFavoriteCount(1L, 5L);
        StockFavoriteCount favorite2 = mockFavoriteCount(2L, 3L);
        given(watchlistFavoriteRepository.countGroupedByStockDesc(any())).willReturn(List.of(favorite1, favorite2));

        Stock stock1 = stockWithId(1L, "005930", "삼성전자");
        Stock stock2 = stockWithId(2L, "000660", "SK하이닉스");
        given(stockRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(stock1, stock2));
        given(kisQuoteClient.fetchQuote("005930")).willThrow(new IllegalStateException("KIS 오류"));
        given(kisQuoteClient.fetchQuote("000660"))
                .willReturn(new KisQuoteResponse.Output("150000", "1000", "2", "0.67", "180000", "100000"));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.POPULAR, 10);

        assertThat(result).extracting(StockRankingResponse::code).containsExactly("000660");
    }

    private StockFavoriteCount mockFavoriteCount(Long stockId, Long favoriteCount) {
        return new StockFavoriteCount() {
            @Override
            public Long getStockId() {
                return stockId;
            }

            @Override
            public Long getFavoriteCount() {
                return favoriteCount;
            }
        };
    }

    private Stock stockWithId(Long id, String code, String name) {
        Stock stock = Stock.builder().code(code).standardCode("KR7" + code + "003").name(name).market(Market.KOSPI).build();
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }
}
