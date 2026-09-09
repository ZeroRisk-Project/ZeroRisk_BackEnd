package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisIndexClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;
import com.zerorisk.project.domain.stock.dto.MarketIndexResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockIndexServiceTest {

    @Mock
    private KisIndexClient kisIndexClient;

    private StockIndexService stockIndexService;

    @DisplayName("코스피/코스닥 지수를 하락(4)/상승(2) 부호에 맞춰 부호를 반영해 반환한다")
    @Test
    void 코스피_코스닥_지수를_부호를_반영해_반환한다() {
        stockIndexService = new StockIndexService(kisIndexClient);
        given(kisIndexClient.fetchIndex(eq("0001")))
                .willReturn(new KisIndexResponse.Output("2682.43", "31.55", "2", "1.19"));
        given(kisIndexClient.fetchIndex(eq("1001")))
                .willReturn(new KisIndexResponse.Output("858.75", "4.12", "4", "-0.48"));

        List<MarketIndexResponse> result = stockIndexService.getIndices();

        assertThat(result).containsExactly(
                new MarketIndexResponse(Market.KOSPI, new BigDecimal("2682.43"), new BigDecimal("31.55"), new BigDecimal("1.19")),
                new MarketIndexResponse(Market.KOSDAQ, new BigDecimal("858.75"), new BigDecimal("-4.12"), new BigDecimal("-0.48")));
    }

    @DisplayName("한 시장 조회가 실패해도 다른 시장 결과는 반환한다")
    @Test
    void 한_시장_조회_실패해도_나머지_결과는_반환한다() {
        stockIndexService = new StockIndexService(kisIndexClient);
        given(kisIndexClient.fetchIndex(eq("0001")))
                .willThrow(new IllegalStateException("KIS 오류"));
        given(kisIndexClient.fetchIndex(eq("1001")))
                .willReturn(new KisIndexResponse.Output("858.75", "4.12", "2", "0.48"));

        List<MarketIndexResponse> result = stockIndexService.getIndices();

        assertThat(result).extracting(MarketIndexResponse::market)
                .containsExactly(Market.KOSDAQ);
    }
}
