package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisQuoteClientImpl implements KisQuoteClient {

    private static final String TR_ID = "FHKST01010100";

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    @Override
    public KisQuoteResponse.Output fetchQuote(String code) {
        KisQuoteResponse response = kisWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", code)
                        .build())
                .header("authorization", "Bearer " + kisTokenService.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisQuoteResponse.class)
                .block();

        if (response == null || response.output() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 시세 조회에 실패했습니다. code=" + code);
        }

        return response.output();
    }
}