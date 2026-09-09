package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisIndexClientImpl implements KisIndexClient {

    private static final String TR_ID = "FHPUP02100000";

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;

    @Override
    public KisIndexResponse.Output fetchIndex(String indexCode) {
        KisIndexResponse response = kisWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", indexCode)
                        .build())
                .header("authorization", "Bearer " + kisTokenService.getAccessToken())
                .header("appkey", kisProperties.appKey())
                .header("appsecret", kisProperties.appSecret())
                .header("tr_id", TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisIndexResponse.class)
                .block();

        if (response == null || response.output() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 지수 조회에 실패했습니다. indexCode=%s, rt_cd=%s, msg1=%s".formatted(
                    indexCode,
                    response == null ? "none" : response.returnCode(),
                    response == null ? "none" : response.message()));
        }

        return response.output();
    }
}
