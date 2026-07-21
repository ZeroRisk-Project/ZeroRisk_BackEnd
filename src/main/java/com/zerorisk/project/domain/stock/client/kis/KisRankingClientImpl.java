package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisRankingClientImpl implements KisRankingClient {

    private static final String TR_ID = "FHPST01710000";

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    @Override
    public List<KisRankingResponse.Output> fetchVolumeRanking() {
        KisRankingResponse response = kisWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                        .queryParam("FID_INPUT_ISCD", "0000")
                        .queryParam("FID_DIV_CLS_CODE", "0")
                        .queryParam("FID_BLNG_CLS_CODE", "0")
                        .queryParam("FID_TRGT_CLS_CODE", "111111111")
                        .queryParam("FID_TRGT_EXLS_CLS_CODE", "000000000")
                        .queryParam("FID_INPUT_PRICE_1", "")
                        .queryParam("FID_INPUT_PRICE_2", "")
                        .queryParam("FID_VOL_CNT", "")
                        .queryParam("FID_INPUT_DATE_1", "")
                        .build())
                .header("authorization", "Bearer " + kisTokenService.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisRankingResponse.class)
                .block();

        if (response == null || response.output() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 거래량순위 조회에 실패했습니다.");
        }

        return response.output();
    }
}