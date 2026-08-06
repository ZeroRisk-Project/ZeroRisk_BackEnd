package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.stock.client.kis.KisChartClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisMinuteChartResponse;
import com.zerorisk.project.domain.stock.dto.ChartCandleResponse;
import com.zerorisk.project.domain.stock.dto.ChartInterval;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockChartServiceTest {

    @Mock
    private KisChartClient kisChartClient;

    private StockChartService stockChartService;

    @DisplayName("DAY 조회 시 일봉 코드로 일별 시세 API 호출")
    @Test
    void DAY_조회_시_일봉_코드로_일별_시세_API_호출() {
        stockChartService = new StockChartService(kisChartClient);
        given(kisChartClient.fetchDailyChart(eq("005930"), eq("D"), anyString(), anyString()))
                .willReturn(List.of(new KisDailyChartResponse.Candle(
                        "20260101", "70000", "72000", "69000", "71000", "1000000")));

        List<ChartCandleResponse> result = stockChartService.getChart("005930", ChartInterval.DAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).open()).isEqualTo(70000L);
        assertThat(result.get(0).close()).isEqualTo(71000L);
        verify(kisChartClient).fetchDailyChart(eq("005930"), eq("D"), anyString(), anyString());
    }

    @DisplayName("WEEK 조회 시 주봉 코드로 일별 시세 API 호출")
    @Test
    void WEEK_조회_시_주봉_코드로_일별_시세_API_호출() {
        stockChartService = new StockChartService(kisChartClient);
        given(kisChartClient.fetchDailyChart(eq("005930"), eq("W"), anyString(), anyString()))
                .willReturn(List.of());

        stockChartService.getChart("005930", ChartInterval.WEEK);

        verify(kisChartClient).fetchDailyChart(eq("005930"), eq("W"), anyString(), anyString());
    }

    @DisplayName("MINUTE 조회 시 분봉 API 호출")
    @Test
    void MINUTE_조회_시_분봉_API_호출() {
        stockChartService = new StockChartService(kisChartClient);
        given(kisChartClient.fetchMinuteChart(eq("005930"), anyString()))
                .willReturn(List.of(new KisMinuteChartResponse.Candle(
                        "20260101", "093000", "70000", "70500", "69800", "70200", "5000")));

        List<ChartCandleResponse> result = stockChartService.getChart("005930", ChartInterval.MINUTE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dateTime()).isEqualTo("20260101093000");
        assertThat(result.get(0).close()).isEqualTo(70200L);
        verify(kisChartClient).fetchMinuteChart(eq("005930"), anyString());
    }
}