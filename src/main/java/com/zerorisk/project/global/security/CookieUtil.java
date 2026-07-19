package com.zerorisk.project.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${cookie.domain:}")
    private String domain;

    public ResponseCookie create(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds);

        if (!domain.isBlank()) {
            builder.domain(domain);
        }
        return builder.build();
    }

    public ResponseCookie delete(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0);

        if (!domain.isBlank()) {
            builder.domain(domain);
        }
        return builder.build();
    }
}