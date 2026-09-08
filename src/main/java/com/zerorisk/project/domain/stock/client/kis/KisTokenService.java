package com.zerorisk.project.domain.stock.client.kis;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KisTokenService {

    private final KisAccessTokenProvider tokenProvider;
    
    public KisTokenService(WebClient kisWebClient, KisProperties kisProperties, KisHttpProperties kisHttpProperties) {
        this.tokenProvider = new KisAccessTokenProvider(
          kisWebClient,
          kisProperties.appKey(),
          kisProperties.appSecret(),
          kisHttpProperties.tokenLockTimeout());
    }

    public String getAccessToken() {
        return tokenProvider.getAccessToken();
    }
}