package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.global.exception.InvalidVerificationCodeException;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "email_code:";
    private static final String VERIFIED_KEY_PREFIX = "email_verified:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;

    public void sendVerificationCode(String email) {
        String code = generateCode();

        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[ZeroRisk] 이메일 인증번호");
        message.setText("인증번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");
        mailSender.send(message);

        log.info("이메일 인증번호 발송 완료 - email: {}", email);
    }

    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new InvalidVerificationCodeException();
        }

        redisTemplate.delete(CODE_KEY_PREFIX + email);
        redisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + email, "true", VERIFIED_TTL);
    }

    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(VERIFIED_KEY_PREFIX + email));
    }

    public void clearVerification(String email) {
        redisTemplate.delete(VERIFIED_KEY_PREFIX + email);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100_000 + random.nextInt(900_000));
    }
}