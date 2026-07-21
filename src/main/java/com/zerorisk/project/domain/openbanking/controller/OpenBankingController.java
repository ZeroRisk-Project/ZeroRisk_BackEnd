package com.zerorisk.project.domain.openbanking.controller;

import com.zerorisk.project.domain.openbanking.dto.AuthenticateAccountResponse;
import com.zerorisk.project.domain.openbanking.dto.BalanceLimitResponse;
import com.zerorisk.project.domain.openbanking.dto.ChargeRequest;
import com.zerorisk.project.domain.openbanking.service.OpenBankingService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/openbanking")
@RequiredArgsConstructor
public class OpenBankingController {

    private final OpenBankingService openBankingService;

    @PostMapping("/authenticate")
    public AuthenticateAccountResponse authenticate(
            @CurrentUserId Long userId,
            @RequestParam(required = false, defaultValue = "mock-code") String code) {
        return openBankingService.authenticateAccount(userId, code);
    }

    @GetMapping("/balance-limit")
    public BalanceLimitResponse getBalanceLimit(@CurrentUserId Long userId) {
        return openBankingService.getAvailableChargeAmount(userId);
    }

    @PostMapping("/recharge")
    public void charge(
            @CurrentUserId Long userId,
            @Valid @RequestBody ChargeRequest request) {
        openBankingService.chargeSeedMoney(userId, request.amount());
    }
}