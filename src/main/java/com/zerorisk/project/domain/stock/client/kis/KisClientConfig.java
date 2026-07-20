package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(KisStockMasterProperties.class)
public class KisClientConfig {

    private static final int MASTER_FILE_MAX_IN_MEMORY_SIZE = 20 * 1024 * 1024;

    @Bean
    public WebClient stockMasterFileWebClient() {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MASTER_FILE_MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}