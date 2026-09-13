package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KisRankingClientImpl implements KisRankingClient {

    private static final String TR_ID = "FHPST01710000";

    // 거래량/거래대금/급상승/급하락 탭이 각자 같은 marketCode를 짧은 간격으로 조회하면 KIS 초당
    // 거래건수 제한(EGW00201)에 걸려 500이 나므로, marketCode별로 짧게 캐싱해 중복 호출을 없앤다.
    private static final Duration CACHE_TTL = Duration.ofSeconds(3);

    private final WebClient kisRankingWebClient;
    private final KisAccessTokenProvider kisRankingTokenProvider;
    private final String appKey;
    private final String appSecret;
    private final Object fetchLock = new Object();
    private final Map<String, CachedRanking> cache = new ConcurrentHashMap<>();

    public KisRankingClientImpl(
      WebClient kisRankingWebClient,
      KisAccessTokenProvider kisRankingTokenProvider,
      KisProperties kisProperties,
      KisRankingProperties kisRankingProperties) {
        this.kisRankingWebClient = kisRankingWebClient;
        this.kisRankingTokenProvider = kisRankingTokenProvider;
        this.appKey = kisRankingProperties.resolveAppKey(kisProperties);
        this.appSecret = kisRankingProperties.resolveAppSecret(kisProperties);
    }

    @Override
    public List<KisRankingResponse.Output> fetchVolumeRanking(String marketCode) {
        synchronized (fetchLock) {
            CachedRanking cached = cache.get(marketCode);
            if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
                return cached.output();
            }

            List<KisRankingResponse.Output> output = requestVolumeRanking(marketCode);
            cache.put(marketCode, new CachedRanking(output, Instant.now().plus(CACHE_TTL)));
            return output;
        }
    }

    private List<KisRankingResponse.Output> requestVolumeRanking(String marketCode) {
        KisRankingResponse response = kisRankingWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                        .queryParam("FID_INPUT_ISCD", marketCode)
                        .queryParam("FID_DIV_CLS_CODE", "0")
                        .queryParam("FID_BLNG_CLS_CODE", "0")
                        .queryParam("FID_TRGT_CLS_CODE", "111111111")
                        .queryParam("FID_TRGT_EXLS_CLS_CODE", "000000000")
                        .queryParam("FID_INPUT_PRICE_1", "")
                        .queryParam("FID_INPUT_PRICE_2", "")
                        .queryParam("FID_VOL_CNT", "")
                        .queryParam("FID_INPUT_DATE_1", "")
                        .build())
                .header("authorization", "Bearer " + kisRankingTokenProvider.getAccessToken())
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", TR_ID)
                .header("custtype", "P")
                .retrieve()
                .bodyToMono(KisRankingResponse.class)
                .block();

        if (response == null || response.output() == null || !"0".equals(response.returnCode())) {
            throw new IllegalStateException("KIS 거래량순위 조회에 실패했습니다. marketCode=%s, rt_cd=%s, msg1=%s".formatted(
              marketCode,
              response == null ? "none" : response.returnCode(),
              response == null ? "none" : response.message()));
        }

        return response.output();
    }

    private record CachedRanking(List<KisRankingResponse.Output> output, Instant expiresAt) {
    }
}