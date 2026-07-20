package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisTokenResponse;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KisTokenService {

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.MIN;

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        return issueToken();
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