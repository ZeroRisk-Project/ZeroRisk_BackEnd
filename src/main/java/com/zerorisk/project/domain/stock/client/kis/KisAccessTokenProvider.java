package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisTokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class KisAccessTokenProvider {

  private static final long EXPIRY_MARGIN_SECONDS = 60;

  // 다른 KIS 클라이언트(시세/차트/랭킹 조회)들과 마찬가지로, 순간적인 실패에 짧게 재시도한다.
  // 토큰 발급은 시세/주문 등 이 앱의 모든 KIS 연동이 공유하는 관문이라, 여기서 재시도 없이
  // 한 번에 실패하면 재시작 직후 등 일시적인 상황에서 종목 조회/주문 체결이 한꺼번에 막힌다.
  private static final int MAX_ATTEMPTS = 3;
  private static final Duration RETRY_DELAY = Duration.ofMillis(500);

  private final WebClient webClient;
  private final String appKey;
  private final String appSecret;
  private final Duration lockTimeout;
  
  private final ReentrantLock issueLock = new ReentrantLock();
  
  private volatile String cachedToken;
  private volatile Instant expiresAt = Instant.MIN;
  
  public KisAccessTokenProvider(WebClient webClient, String appKey, String appSecret, Duration lockTimeout) {
    this.webClient = webClient;
    this.appKey = appKey;
    this.appSecret = appSecret;
    this.lockTimeout = lockTimeout;
  }
  
  public String getAccessToken() {
    String cached = cachedTokenIfValid();
    if (cached != null) {
      return cached;
    }
    
    boolean acquired;
    try {
      acquired = issueLock.tryLock(lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
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
    IllegalStateException failure = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      Map<String, String> body = Map.of(
        "grant_type", "client_credentials",
        "appkey", appKey,
        "appsecret", appSecret);

      KisTokenResponse response = webClient.post()
        .uri("/oauth2/tokenP")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(KisTokenResponse.class)
        .onErrorResume(e -> Mono.empty())
        .block();

      if (response != null && response.accessToken() != null) {
        this.cachedToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - EXPIRY_MARGIN_SECONDS, 0));
        return cachedToken;
      }

      failure = new IllegalStateException("KIS 액세스 토큰 발급에 실패했습니다.");
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