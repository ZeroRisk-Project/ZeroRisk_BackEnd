package com.zerorisk.project.domain.stock.client.kis;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties({
        KisStockMasterProperties.class,
        KisProperties.class,
        KisRankingProperties.class,
        KisRealtimeProperties.class,
        KisHttpProperties.class})
public class KisClientConfig {

    private static final int MASTER_FILE_MAX_IN_MEMORY_SIZE = 20 * 1024 * 1024;

    @Bean
    public WebClient stockMasterFileWebClient(KisHttpProperties kisHttpProperties) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MASTER_FILE_MAX_IN_MEMORY_SIZE))
                .build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient(
                        "kis-master-file",
                        kisHttpProperties,
                        kisHttpProperties.masterFileResponseTimeout())))
                .build();
    }

    @Bean
    public WebClient kisWebClient(KisProperties kisProperties, KisHttpProperties kisHttpProperties) {
        return WebClient.builder()
                .baseUrl(kisProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient(
                        "kis-api",
                        kisHttpProperties,
                        kisHttpProperties.responseTimeout())))
                .build();
    }
    
    @Bean
    public WebClient kisRankingWebClient(
      KisProperties kisProperties,
      KisRankingProperties kisRankingProperties,
      KisHttpProperties kisHttpProperties) {
        return WebClient.builder()
          .baseUrl(kisRankingProperties.resolveBaseUrl(kisProperties))
          .clientConnector(new ReactorClientHttpConnector(httpClient(
            "kis-ranking-api",
            kisHttpProperties,
            kisHttpProperties.responseTimeout())))
          .build();
    }
    
    @Bean
    public KisAccessTokenProvider kisRankingTokenProvider(
      WebClient kisRankingWebClient,
      KisProperties kisProperties,
      KisRankingProperties kisRankingProperties,
      KisHttpProperties kisHttpProperties) {
        return new KisAccessTokenProvider(
          kisRankingWebClient,
          kisRankingProperties.resolveAppKey(kisProperties),
          kisRankingProperties.resolveAppSecret(kisProperties),
          kisHttpProperties.tokenLockTimeout());
    }
    
    private HttpClient httpClient(String poolName, KisHttpProperties properties, Duration responseTimeout) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder(poolName)
                .maxConnections(properties.maxConnections())
                .pendingAcquireTimeout(properties.pendingAcquireTimeout())
                .maxIdleTime(properties.maxIdleTime())
                .evictInBackground(properties.maxIdleTime())
                .build();

        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
                .responseTimeout(responseTimeout)
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeout.toMillis(), TimeUnit.MILLISECONDS)));
    }
}