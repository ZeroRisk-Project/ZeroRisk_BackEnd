// 수정 후
package com.zerorisk.project.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${cookie.domain:}")
    private String domain;

    // 배포(https)는 true, 로컬(http)은 application-local.properties에서 false로 오버라이드
    @Value("${cookie.secure:true}")
    private boolean secure;

    public ResponseCookie create(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
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
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0);

        if (!domain.isBlank()) {
            builder.domain(domain);
        }
        return builder.build();
    }
}