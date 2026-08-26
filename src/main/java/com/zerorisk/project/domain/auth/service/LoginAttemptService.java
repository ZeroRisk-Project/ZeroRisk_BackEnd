package com.zerorisk.project.domain.auth.service;

import com.zerorisk.project.global.security.ratelimit.SlidingWindowCounter;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인 실패 횟수를 계정/IP 단위로 추적해 CAPTCHA 요구 여부를 판단한다.
 * 계정 잠금은 두지 않는다 — 타인이 특정 계정을 고의로 잠가버리는 서비스 거부 공격을 방지하기 위함.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String ACCOUNT_KEY_PREFIX = "login_fail:account:";
    private static final String IP_KEY_PREFIX = "login_fail:ip:";
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int ACCOUNT_CAPTCHA_THRESHOLD = 5;
    private static final int IP_CAPTCHA_THRESHOLD = 20;

    private final SlidingWindowCounter slidingWindowCounter;

    public boolean isCaptchaRequired(String email, String clientIp) {
        long accountFailures = slidingWindowCounter.count(ACCOUNT_KEY_PREFIX + email, WINDOW);
        long ipFailures = slidingWindowCounter.count(IP_KEY_PREFIX + clientIp, WINDOW);
        return accountFailures >= ACCOUNT_CAPTCHA_THRESHOLD || ipFailures >= IP_CAPTCHA_THRESHOLD;
    }

    public void recordFailure(String email, String clientIp) {
        slidingWindowCounter.record(ACCOUNT_KEY_PREFIX + email, WINDOW);
        slidingWindowCounter.record(IP_KEY_PREFIX + clientIp, WINDOW);
    }
}
