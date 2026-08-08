package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisMinuteChartResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisChartClientImpl implements KisChartClient {

    private static final String DAILY_TR_ID = "FHKST03010100";
    private static final String MINUTE_TR_ID = "FHKST03010200";

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    @Override
    public List<KisDailyChartResponse.Candle> fetchDailyChart(
            String code, String periodCode, String startDate, String endDate) {
        KisDailyChartResponse response = kisWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", code)
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_PERIOD_DIV_CODE", periodCode)
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build())
                .header("authorization", "Bearer " + kisTokenService.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", DAILY_TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisDailyChartResponse.class)
                .block();

        if (response == null || response.candles() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 기간별 시세 조회에 실패했습니다. code=" + code);
        }

        return response.candles();
    }

    @Override
    public List<KisMinuteChartResponse.Candle> fetchMinuteChart(String code, String baseTime) {
        KisMinuteChartResponse response = kisWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice")
                        .queryParam("FID_ETC_CLS_CODE", "")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", code)
                        .queryParam("FID_INPUT_HOUR_1", baseTime)
                        .queryParam("FID_PW_DATA_INCU_YN", "Y")
                        .build())
                .header("authorization", "Bearer " + kisTokenService.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", MINUTE_TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisMinuteChartResponse.class)
                .block();

        if (response == null || response.candles() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 분봉 조회에 실패했습니다. code=" + code);
        }

        return response.candles();
    }
}