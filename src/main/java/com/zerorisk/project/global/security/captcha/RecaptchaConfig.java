package com.zerorisk.project.global.security.captcha;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RecaptchaConfig {

    @Bean
    public WebClient recaptchaWebClient() {
        return WebClient.builder()
                .baseUrl("https://www.google.com")
                .build();
    }
}
