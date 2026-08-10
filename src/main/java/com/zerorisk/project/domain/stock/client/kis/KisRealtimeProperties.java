package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis.realtime")
public record KisRealtimeProperties(String wsUrl) {

}