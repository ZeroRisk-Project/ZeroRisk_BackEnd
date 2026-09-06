package com.zerorisk.project.domain.stock.client.kis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "kis.http")
public record KisHttpProperties(
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("5s") Duration responseTimeout,
        @DefaultValue("60s") Duration masterFileResponseTimeout,
        @DefaultValue("50") int maxConnections,
        @DefaultValue("5s") Duration pendingAcquireTimeout,
        @DefaultValue("30s") Duration maxIdleTime,
        @DefaultValue("3s") Duration tokenLockTimeout) {
}