package com.zerorisk.project.domain.auth.service;

import com.zerorisk.project.domain.auth.dto.LoginRequest;
import com.zerorisk.project.domain.auth.dto.LoginResponse;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.InvalidCredentialsException;
import com.zerorisk.project.global.exception.InvalidRefreshTokenException;
import com.zerorisk.project.global.security.JwtTokenProvider;
import com.zerorisk.project.global.security.OpaqueTokenGenerator;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMillis;

    @Transactional(readOnly = true)
    public TokenResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

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
}