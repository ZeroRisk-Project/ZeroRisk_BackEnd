package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisOrderBookClient;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisOrderBookResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.dto.OrderBookResponse;
import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.dto.StockQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockQueryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    @Mock
    private KisOrderBookClient kisOrderBookClient;

    private StockQueryService stockQueryService;

    @DisplayName("하락 부호일 때 변동 금액을 음수로 변환")
    @Test
    void 하락_부호일_때_변동_금액을_음수로_변환() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient, kisOrderBookClient);
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "70000", "1000", "5", "-1.41", "88800", "49900"));

        StockDetailResponse result = stockQueryService.getDetail("005930");

        assertThat(result.currentPrice()).isEqualTo(70000L);
        assertThat(result.changeAmount()).isEqualTo(-1000L);
        assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("-1.41"));
        assertThat(result.week52High()).isEqualTo(88800L);
        assertThat(result.week52Low()).isEqualTo(49900L);
    }

    @DisplayName("상승 부호일 때 변동 금액을 양수로 유지")
    @Test
    void 상승_부호일_때_변동_금액을_양수로_유지() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient, kisOrderBookClient);
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "72000", "1000", "2", "1.41", "88800", "49900"));

        StockDetailResponse result = stockQueryService.getDetail("005930");

        assertThat(result.changeAmount()).isEqualTo(1000L);
    }

    @DisplayName("시세 조회 시 일부 종목이 실패해도 나머지 결과는 반환")
    @Test
    void 시세_조회_시_일부_종목이_실패해도_나머지_결과는_반환() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient, kisOrderBookClient);
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "72000", "1000", "2", "1.41", "88800", "49900"));
        given(kisQuoteClient.fetchQuote("000660")).willThrow(new RuntimeException("조회 실패"));

        List<StockQuoteResponse> result = stockQueryService.getQuotes(List.of("005930", "000660"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("005930");
        assertThat(result.get(0).changeAmount()).isEqualTo(1000L);
        assertThat(result.get(0).changeRate()).isEqualByComparingTo(new BigDecimal("1.41"));
    }

    @DisplayName("존재하지 않는 종목 코드면 예외 발생")
    @Test
    void 존재하지_않는_종목_코드면_예외_발생() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient, kisOrderBookClient);
        given(stockRepository.findByCode("999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockQueryService.getDetail("999999"))
                .isInstanceOf(StockNotFoundException.class);
    }

    @DisplayName("호가 조회 시 매도/매수 호가를 현재가에 가까운 순으로 정렬해서 반환")
    @Test
    void 호가_조회_시_매도_매수_호가를_현재가에_가까운_순으로_정렬해서_반환() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient, kisOrderBookClient);
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock));
        given(kisOrderBookClient.fetchOrderBook("005930")).willReturn(new KisOrderBookResponse.Output1(
                "270000", "270500", "271000", "271500", "272000", "272500", "273000", "273500", "274000", "274500",
                "269500", "269000", "268500", "268000", "267500", "267000", "266500", "266000", "265500", "265000",
                "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000",
                "1100", "1200", "1300", "1400", "1500", "1600", "1700", "1800", "1900", "2000",
                "5500", "15500"));

        OrderBookResponse result = stockQueryService.getOrderBook("005930");

        assertThat(result.sellLevels()).hasSize(10);
        assertThat(result.sellLevels().get(0).price()).isEqualTo(274500L); // 10단계(가장 먼 호가)가 배열 맨 앞
        assertThat(result.sellLevels().get(9).price()).isEqualTo(270000L); // 1단계(가장 가까운 호가)가 배열 맨 뒤
        assertThat(result.buyLevels()).hasSize(10);
        assertThat(result.buyLevels().get(0).price()).isEqualTo(269500L); // 1단계(가장 가까운 호가)가 배열 맨 앞
        assertThat(result.buyLevels().get(9).price()).isEqualTo(265000L); // 10단계(가장 먼 호가)가 배열 맨 뒤
        assertThat(result.totalSellQuantity()).isEqualTo(5500L);
        assertThat(result.totalBuyQuantity()).isEqualTo(15500L);
    }
}
