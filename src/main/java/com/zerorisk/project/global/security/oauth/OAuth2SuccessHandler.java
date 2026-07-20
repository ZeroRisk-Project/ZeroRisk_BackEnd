package com.zerorisk.project.global.security.oauth;

import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.security.CookieUtil;
import com.zerorisk.project.global.security.JwtTokenProvider;
import com.zerorisk.project.global.security.OpaqueTokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final long ACCESS_TOKEN_MAX_AGE = 60 * 30;
    private static final long REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7;

    private final JwtTokenProvider jwtTokenProvider;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final RedisTemplate<String, String> redisTemplate;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMillis;

    @Value("${oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String email = extractEmail(authentication.getPrincipal());

        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("OAuth 인증 후 사용자를 찾을 수 없습니다."))
                .getId();

        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = opaqueTokenGenerator.generate();

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshToken,
                String.valueOf(userId),
                Duration.ofMillis(refreshTokenExpirationMillis));

        response.addHeader("Set-Cookie",
                cookieUtil.create("accessToken", accessToken, ACCESS_TOKEN_MAX_AGE).toString());
        response.addHeader("Set-Cookie",
                cookieUtil.create("refreshToken", refreshToken, REFRESH_TOKEN_MAX_AGE).toString());

        response.sendRedirect(successRedirectUrl);
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            if (email != null) {
                return (String) email;
            }
            Object kakaoAccount = oauth2User.getAttributes().get("kakao_account");
            if (kakaoAccount instanceof Map<?, ?> map) {
                return (String) map.get("email");
            }
        }
        throw new IllegalStateException("OAuth2 principal에서 이메일을 추출할 수 없습니다.");
    }
}