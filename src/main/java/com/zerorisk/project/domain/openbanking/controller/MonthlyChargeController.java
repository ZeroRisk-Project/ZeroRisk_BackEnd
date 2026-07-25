package com.zerorisk.project.domain.openbanking.controller;

import com.zerorisk.project.domain.openbanking.dto.MonthlyChargeSettingRequest;
import com.zerorisk.project.domain.openbanking.dto.MonthlyChargeSettingResponse;
import com.zerorisk.project.domain.openbanking.service.OpenBankingService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/openbanking/monthly-charge")
@RequiredArgsConstructor
public class MonthlyChargeController {

    private final OpenBankingService openBankingService;

    @PostMapping
    public MonthlyChargeSettingResponse register(
            @CurrentUserId Long userId,
            @Valid @RequestBody MonthlyChargeSettingRequest request) {
        return openBankingService.registerMonthlyCharge(userId, request);
    }

    @GetMapping
    public MonthlyChargeSettingResponse get(@CurrentUserId Long userId) {
        return openBankingService.getMonthlyCharge(userId);
    }

    @PutMapping
    public MonthlyChargeSettingResponse update(
            @CurrentUserId Long userId,
            @Valid @RequestBody MonthlyChargeSettingRequest request) {
        return openBankingService.updateMonthlyCharge(userId, request);
    }

    @PatchMapping("/deactivate")
    public void deactivate(@CurrentUserId Long userId) {
        openBankingService.deactivateMonthlyCharge(userId);
    }

    @PatchMapping("/activate")
    public void activate(@CurrentUserId Long userId) {
        openBankingService.activateMonthlyCharge(userId);
    }
}