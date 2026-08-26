package com.zerorisk.project.global.security.captcha;

import com.zerorisk.project.global.exception.CaptchaRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class RecaptchaVerifier {

    private final WebClient recaptchaWebClient;

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new CaptchaRequiredException();
        }

        RecaptchaResponse response = recaptchaWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/recaptcha/api/siteverify")
                        .queryParam("secret", secretKey)
                        .queryParam("response", token)
                        .build())
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .block();

        if (response == null || !response.success()) {
            throw new CaptchaRequiredException();
        }
    }

    private record RecaptchaResponse(boolean success) {
    }
}
