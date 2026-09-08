package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis.ranking")
public record KisRankingProperties(String baseUrl, String appKey, String appSecret) {
  
  public String resolveBaseUrl(KisProperties defaults) {
    return hasCredentials() && hasText(baseUrl) ? baseUrl : defaults.baseUrl();
  }
  
  public String resolveAppKey(KisProperties defaults) {
    return hasCredentials() ? appKey : defaults.appKey();
  }
  
  public String resolveAppSecret(KisProperties defaults) {
    return hasCredentials() ? appSecret : defaults.appSecret();
  }
  
  public boolean hasCredentials() {
    return hasText(appKey) && hasText(appSecret);
  }
  
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}