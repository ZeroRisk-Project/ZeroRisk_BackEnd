package com.zerorisk.project.domain.stock.client.kis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KisRankingPropertiesTest {
  
  private static final KisProperties DEFAULTS =
    new KisProperties("https://openapivts.koreainvestment.com:29443", "vts-key", "vts-secret");
  
  @DisplayName("실전 앱키가 모두 설정되면 랭킹 전용 설정을 사용")
  @Test
  void 실전_앱키가_모두_설정되면_랭킹_전용_설정을_사용() {
    KisRankingProperties properties = new KisRankingProperties(
      "https://openapi.koreainvestment.com:9443", "real-key", "real-secret");
    
    assertThat(properties.hasCredentials()).isTrue();
    assertThat(properties.resolveBaseUrl(DEFAULTS)).isEqualTo("https://openapi.koreainvestment.com:9443");
    assertThat(properties.resolveAppKey(DEFAULTS)).isEqualTo("real-key");
    assertThat(properties.resolveAppSecret(DEFAULTS)).isEqualTo("real-secret");
  }
  
  @DisplayName("실전 앱키가 없으면 기본 설정을 사용")
  @Test
  void 실전_앱키가_없으면_기본_설정을_사용() {
    KisRankingProperties properties = new KisRankingProperties(
      "https://openapi.koreainvestment.com:9443", "", "");
    
    assertThat(properties.hasCredentials()).isFalse();
    assertThat(properties.resolveBaseUrl(DEFAULTS)).isEqualTo(DEFAULTS.baseUrl());
    assertThat(properties.resolveAppKey(DEFAULTS)).isEqualTo(DEFAULTS.appKey());
    assertThat(properties.resolveAppSecret(DEFAULTS)).isEqualTo(DEFAULTS.appSecret());
  }
  
  @DisplayName("앱키만 있으면 기본 설정을 사용")
  @Test
  void 앱키만_있으면_기본_설정을_사용() {
    KisRankingProperties properties = new KisRankingProperties(
      "https://openapi.koreainvestment.com:9443", "real-key", null);
    
    assertThat(properties.hasCredentials()).isFalse();
    assertThat(properties.resolveBaseUrl(DEFAULTS)).isEqualTo(DEFAULTS.baseUrl());
    assertThat(properties.resolveAppKey(DEFAULTS)).isEqualTo(DEFAULTS.appKey());
  }
  
  @DisplayName("도메인이 비어 있으면 실전 앱키가 있어도 기본 도메인을 사용")
  @Test
  void 도메인이_비어_있으면_실전_앱키가_있어도_기본_도메인을_사용() {
    KisRankingProperties properties = new KisRankingProperties("", "real-key", "real-secret");
    
    assertThat(properties.resolveBaseUrl(DEFAULTS)).isEqualTo(DEFAULTS.baseUrl());
    assertThat(properties.resolveAppKey(DEFAULTS)).isEqualTo("real-key");
  }
}