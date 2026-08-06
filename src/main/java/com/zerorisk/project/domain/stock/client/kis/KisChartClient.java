package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisMinuteChartResponse;
import java.util.List;

public interface KisChartClient {

    List<KisDailyChartResponse.Candle> fetchDailyChart(String code, String periodCode, String startDate, String endDate);

    List<KisMinuteChartResponse.Candle> fetchMinuteChart(String code, String baseTime);
}