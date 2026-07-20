package com.zerorisk.project.domain.auth.controller;

import com.zerorisk.project.domain.auth.dto.KakaoUnlinkWebhookRequest;
import com.zerorisk.project.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/kakao")
@RequiredArgsConstructor
public class KakaoWebhookController {

    private final AuthService authService;

    @PostMapping("/unlink")
    public ResponseEntity<Void> unlink(@RequestBody KakaoUnlinkWebhookRequest request) {
        authService.handleKakaoUnlink(request.user_id());
        return ResponseEntity.ok().build();
    }
}