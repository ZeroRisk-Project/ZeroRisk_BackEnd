package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisMinuteChartResponse;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisChartClientImpl implements KisChartClient {

    private static final String DAILY_TR_ID = "FHKST03010100";
    private static final String MINUTE_TR_ID = "FHKST03010200";

    // 종목 비교처럼 여러 종목을 잇달아 조회할 때 KIS가 순간적인 요청 폭주를 이유로
    // 일부 요청만 rt_cd 오류로 실패시키는 경우가 있어, 짧게 한 번 재시도한다.
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    @Override
    public List<KisDailyChartResponse.Candle> fetchDailyChart(
            String code, String periodCode, String startDate, String endDate) {
        IllegalStateException failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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

            if (response != null && response.candles() != null && "0".equals(response.returnCode())) {
                return response.candles();
            }

            failure = new IllegalStateException("KIS 기간별 시세 조회에 실패했습니다. code=%s, rt_cd=%s, msg1=%s".formatted(
                    code,
                    response == null ? "none" : response.returnCode(),
                    response == null ? "none" : response.message()));
            sleepBeforeRetry(attempt);
        }
        throw failure;
    }

    @Override
    public List<KisMinuteChartResponse.Candle> fetchMinuteChart(String code, String baseTime) {
        IllegalStateException failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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

            if (response != null && response.candles() != null && "0".equals(response.returnCode())) {
                return response.candles();
            }

            failure = new IllegalStateException("KIS 분봉 조회에 실패했습니다. code=%s, rt_cd=%s, msg1=%s".formatted(
                    code,
                    response == null ? "none" : response.returnCode(),
                    response == null ? "none" : response.message()));
            sleepBeforeRetry(attempt);
        }
        throw failure;
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}