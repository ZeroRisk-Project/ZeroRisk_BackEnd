package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisTokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class KisAccessTokenProvider {
  
  private static final long EXPIRY_MARGIN_SECONDS = 60;
  
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
      .block();
    
    if (response == null || response.accessToken() == null) {
      throw new IllegalStateException("KIS 액세스 토큰 발급에 실패했습니다.");
    }
    
    this.cachedToken = response.accessToken();
    this.expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - EXPIRY_MARGIN_SECONDS, 0));
    
    return cachedToken;
  }
}