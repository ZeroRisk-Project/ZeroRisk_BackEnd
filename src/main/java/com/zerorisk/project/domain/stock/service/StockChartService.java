package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisChartClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisMinuteChartResponse;
import com.zerorisk.project.domain.stock.dto.ChartCandleResponse;
import com.zerorisk.project.domain.stock.dto.ChartInterval;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockChartService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final int DAY_LOOKBACK_DAYS = 100;
    private static final int WEEK_LOOKBACK_DAYS = 700;
    private static final int MONTH_LOOKBACK_DAYS = 3000;

    private final KisChartClient kisChartClient;

    public List<ChartCandleResponse> getChart(String code, ChartInterval interval) {
        if (interval == ChartInterval.MINUTE) {
            return getMinuteChart(code);
        }
        return getDailyChart(code, interval);
    }

    private List<ChartCandleResponse> getDailyChart(String code, ChartInterval interval) {
        record ChartRange(String periodCode, int lookbackDays) {
        }

        ChartRange range = switch (interval) {
            case DAY -> new ChartRange("D", DAY_LOOKBACK_DAYS);
            case WEEK -> new ChartRange("W", WEEK_LOOKBACK_DAYS);
            case MONTH -> new ChartRange("M", MONTH_LOOKBACK_DAYS);
            case MINUTE -> throw new IllegalArgumentException("분봉은 별도 API로 조회합니다.");
        };

        LocalDate today = LocalDate.now();
        String startDate = today.minusDays(range.lookbackDays()).format(DATE_FORMAT);
        String endDate = today.format(DATE_FORMAT);

        return kisChartClient.fetchDailyChart(code, range.periodCode(), startDate, endDate).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<ChartCandleResponse> getMinuteChart(String code) {
        String baseTime = LocalTime.now().format(TIME_FORMAT);

        return kisChartClient.fetchMinuteChart(code, baseTime).stream()
                .map(this::toResponse)
                .toList();
    }

    private ChartCandleResponse toResponse(KisDailyChartResponse.Candle candle) {
        return new ChartCandleResponse(
                candle.date(),
                Long.parseLong(candle.open()),
                Long.parseLong(candle.high()),
                Long.parseLong(candle.low()),
                Long.parseLong(candle.close()),
                Long.parseLong(candle.volume()));
    }

    private ChartCandleResponse toResponse(KisMinuteChartResponse.Candle candle) {
        return new ChartCandleResponse(
                candle.date() + candle.time(),
                Long.parseLong(candle.open()),
                Long.parseLong(candle.high()),
                Long.parseLong(candle.low()),
                Long.parseLong(candle.close()),
                Long.parseLong(candle.volume()));
    }
}