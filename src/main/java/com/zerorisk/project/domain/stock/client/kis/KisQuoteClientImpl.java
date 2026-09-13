package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KisQuoteClientImpl implements KisQuoteClient {

    private static final String TR_ID = "FHKST01010100";

    // 종목 비교처럼 여러 종목을 잇달아 조회할 때 KIS가 순간적인 요청 폭주를 이유로
    // 일부 요청만 rt_cd 오류로 실패시키는 경우가 있어, 짧게 한 번 재시도한다.
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);

    // "전체보기" 목록과 상세 페이지가 짧은 시간에 같은 종목을 중복 조회하는 경우가 많아,
    // 종목코드별로 짧게 캐싱해 불필요한 KIS 호출과 응답 지연을 줄인다.
    private static final Duration CACHE_TTL = Duration.ofSeconds(2);

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    @Override
    public KisQuoteResponse.Output fetchQuote(String code) {
        CachedQuote cached = cache.get(code);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.output();
        }

        KisQuoteResponse.Output output = requestQuote(code);
        cache.put(code, new CachedQuote(output, Instant.now().plus(CACHE_TTL)));
        return output;
    }

    private KisQuoteResponse.Output requestQuote(String code) {
        IllegalStateException failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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

            if (response != null && response.output() != null && "0".equals(response.returnCode())) {
                return response.output();
            }

            failure = new IllegalStateException("KIS 시세 조회에 실패했습니다. code=%s, rt_cd=%s, msg1=%s".formatted(
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

    private record CachedQuote(KisQuoteResponse.Output output, Instant expiresAt) {
    }
}
