package com.zerorisk.project.domain.user.controller;

import com.zerorisk.project.domain.auth.dto.LoginRequest;
import com.zerorisk.project.domain.auth.dto.LoginResponse;
import com.zerorisk.project.domain.auth.service.AuthService;
import com.zerorisk.project.domain.user.dto.NicknameCheckResponse;
import com.zerorisk.project.domain.user.dto.SendCodeRequest;
import com.zerorisk.project.domain.user.dto.SignupRequest;
import com.zerorisk.project.domain.user.dto.SignupResponse;
import com.zerorisk.project.domain.user.dto.VerifyCodeRequest;
import com.zerorisk.project.domain.user.service.EmailVerificationService;
import com.zerorisk.project.domain.user.service.UserService;
import com.zerorisk.project.global.audit.ClientIpExtractor;
import com.zerorisk.project.global.exception.InvalidRefreshTokenException;
import com.zerorisk.project.global.security.CookieUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private static final long ACCESS_TOKEN_MAX_AGE = 60 * 30;
    private static final long REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 7;

    private final UserService userService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        String clientIp = ClientIpExtractor.extract();
        SignupResponse response = userService.signup(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String clientIp = ClientIpExtractor.extract();
        AuthService.TokenResult result = authService.login(request, clientIp);
        return withTokenCookies(result);
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        AuthService.TokenResult result = authService.reissue(refreshToken);
        return withTokenCookies(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(refreshToken);

        ResponseCookie deletedAccess = cookieUtil.delete("accessToken");
        ResponseCookie deletedRefresh = cookieUtil.delete("refreshToken");

        return ResponseEntity.ok()
                .header("Set-Cookie", deletedAccess.toString())
                .header("Set-Cookie", deletedRefresh.toString())
                .build();
    }

    private ResponseEntity<LoginResponse> withTokenCookies(AuthService.TokenResult result) {
        ResponseCookie accessCookie = cookieUtil.create("accessToken", result.accessToken(), ACCESS_TOKEN_MAX_AGE);
        ResponseCookie refreshCookie = cookieUtil.create("refreshToken", result.refreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(result.response());
    }

    @GetMapping("/nickname-check")
    public NicknameCheckResponse checkNickname(
            @RequestParam @NotBlank @Size(max = 12) String nickname) {
        return userService.checkNickname(nickname);
    }

    @PostMapping("/send-code")
    public void sendCode(@Valid @RequestBody SendCodeRequest request) {
        emailVerificationService.sendVerificationCode(request.email());
    }

    @PostMapping("/verify-code")
    public void verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
    }
}