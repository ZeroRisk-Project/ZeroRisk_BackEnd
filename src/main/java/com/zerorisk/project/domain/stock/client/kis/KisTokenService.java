package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisTokenResponse;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KisTokenService {

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;
    private final KisHttpProperties kisHttpProperties;

    private final ReentrantLock issueLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.MIN;

    public String getAccessToken() {
        String cached = cachedTokenIfValid();
        if (cached != null) {
            return cached;
        }
        boolean acquired;
        try {
            acquired = issueLock.tryLock(kisHttpProperties.tokenLockTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("KIS 액세스 토큰 발급 대기 중 인터럽트되었습니다.", e);
        }

        if (!acquired) {
            throw new IllegalStateException("KIS 액세스 토큰 발급 대기 시간을 초과했습니다.");
        }

        try {
            String issued = cachedTokenIfValid();
            return issued != null ? issued : issueToken();
        } finally {
            issueLock.unlock();
        }
    }

    private String cachedTokenIfValid() {
        String token = cachedToken;
        return token != null && Instant.now().isBefore(expiresAt) ? token : null;
    }

    private String issueToken() {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisProperties.appKey(),
                "appsecret", kisProperties.appSecret());

        KisTokenResponse response = kisWebClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .block();

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("KIS 액세스 토큰 발급에 실패했습니다.");
        }

        this.cachedToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - 60, 0));

        return cachedToken;
    }
}