package com.zerorisk.project.domain.auth.service;

import com.zerorisk.project.domain.auth.dto.LoginRequest;
import com.zerorisk.project.domain.auth.dto.LoginResponse;
import com.zerorisk.project.domain.user.entity.OAuthProvider;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.InvalidCredentialsException;
import com.zerorisk.project.global.exception.InvalidRefreshTokenException;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.security.JwtTokenProvider;
import com.zerorisk.project.global.security.OpaqueTokenGenerator;
import com.zerorisk.project.global.security.captcha.RecaptchaVerifier;
import com.zerorisk.project.global.exception.InvalidKakaoWebhookException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    // 이메일이 존재하지 않을 때도 BCrypt 비교를 수행시켜, 존재/미존재 응답 시간 차이로
    // 이메일 존재 여부가 유추되지 않도록 한다(타이밍 사이드채널 방지).
    private static final String DUMMY_PASSWORD_HASH =
            new BCryptPasswordEncoder().encode("dummy-password-for-constant-time-login");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserActivityLogger userActivityLogger;
    private final LoginAttemptService loginAttemptService;
    private final RecaptchaVerifier recaptchaVerifier;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMillis;

    @Value("${kakao.app-id:}")
    private String kakaoAppId;

    @Transactional(readOnly = true)
    public TokenResult login(LoginRequest request, String clientIp) {
        if (loginAttemptService.isCaptchaRequired(request.email(), clientIp)) {
            recaptchaVerifier.verify(request.recaptchaToken());
        }

        User user = userRepository.findByEmail(request.email()).orElse(null);

        String passwordHashToCheck = (user != null && user.getPassword() != null)
                ? user.getPassword()
                : DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHashToCheck);

        if (user == null || user.getPassword() == null || !passwordMatches) {
            loginAttemptService.recordFailure(request.email(), clientIp);
            throw new InvalidCredentialsException();
        }

        userActivityLogger.log(user.getId(), "LOGIN", "로그인");
        return issueTokens(user);
    }

    public TokenResult reissue(String refreshToken) {
        String redisKey = REFRESH_KEY_PREFIX + refreshToken;
        String userIdValue = redisTemplate.opsForValue().get(redisKey);

        if (userIdValue == null) {
            throw new InvalidRefreshTokenException();
        }

        // 회전(Rotation): 기존 토큰은 즉시 폐기하고 새 토큰 세트를 발급한다.
        redisTemplate.delete(redisKey);

        Long userId = Long.parseLong(userIdValue);
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidRefreshTokenException::new);

        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            redisTemplate.delete(REFRESH_KEY_PREFIX + refreshToken);
        }
    }

    private TokenResult issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = opaqueTokenGenerator.generate();

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                Duration.ofMillis(refreshTokenExpirationMillis));

        LoginResponse response = new LoginResponse(user.getId(), user.getEmail(), user.getNickname());
        return new TokenResult(response, accessToken, refreshToken);
    }

    public record TokenResult(LoginResponse response, String accessToken, String refreshToken) {
    }

    @Transactional
    public void handleKakaoUnlink(String appId, String kakaoUserId) {
        if (kakaoAppId.isBlank() || !kakaoAppId.equals(appId)) {
            throw new InvalidKakaoWebhookException();
        }

        userRepository.findByOauthProviderAndOauthProviderId(OAuthProvider.KAKAO, kakaoUserId)
                .ifPresent(User::withdraw);
    }
}