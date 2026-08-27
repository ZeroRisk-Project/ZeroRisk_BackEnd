package com.zerorisk.project.global.security.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    // OAuth2 인증 흐름(리다이렉트 왕복)은 보통 몇 초~몇십 초 안에 끝나므로,
    // 탈취되더라도 악용 가능한 시간 창을 최소화하기 위해 짧게 설정
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    // 배포(https)는 true, 로컬(http)은 application-local.properties에서 false로 오버라이드 (CookieUtil과 동일한 컨벤션)
    @Value("${cookie.secure:true}")
    private boolean secure;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request)
                .map(cookie -> CookieSerializationUtils.deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookie(response);
            return;
        }

        String serialized = CookieSerializationUtils.serialize(authorizationRequest);

        ResponseCookie cookie = ResponseCookie.from(OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized)
                .httpOnly(true)   // 자바스크립트로 접근 불가 (XSS로 인한 탈취 방지)
                .secure(secure)   // HTTPS에서만 전송 (평문 노출 방지, 로컬 개발 시 cookie.secure=false로 오버라이드)
                .sameSite("Lax")  // 외부(Google/Kakao)에서 돌아오는 정상 리다이렉트는 허용하되,
                                  // 위험한 크로스사이트 POST 요청에는 여전히 방어됨
                .path("/")
                .maxAge(COOKIE_MAX_AGE_SECONDS)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookie(response);
        return authorizationRequest;
    }

    private void removeAuthorizationRequestCookie(HttpServletResponse response) {
        // 사용 후(성공/실패 무관) 즉시 삭제 - 재사용 공격 여지 차단
        ResponseCookie deleteCookie = ResponseCookie.from(OAUTH2_AUTH_REQUEST_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());
    }

    private Optional<Cookie> getCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> OAUTH2_AUTH_REQUEST_COOKIE_NAME.equals(c.getName()))
                .filter(c -> StringUtils.hasText(c.getValue()))
                .findFirst();
    }
}
