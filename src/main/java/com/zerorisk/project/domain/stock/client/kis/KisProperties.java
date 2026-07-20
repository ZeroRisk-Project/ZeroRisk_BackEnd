package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis")
public record KisProperties(String baseUrl, String appKey, String appSecret) {

}