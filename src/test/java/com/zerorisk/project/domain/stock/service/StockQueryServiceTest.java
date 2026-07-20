package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.dto.StockDetailResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
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

    private StockQueryService stockQueryService;

    @DisplayName("하락 부호일 때 변동 금액을 음수로 변환")
    @Test
    void 하락_부호일_때_변동_금액을_음수로_변환() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient);
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
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient);
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

    @DisplayName("존재하지 않는 종목 코드면 예외 발생")
    @Test
    void 존재하지_않는_종목_코드면_예외_발생() {
        stockQueryService = new StockQueryService(stockRepository, kisQuoteClient);
        given(stockRepository.findByCode("999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockQueryService.getDetail("999999"))
                .isInstanceOf(StockNotFoundException.class);
    }
}