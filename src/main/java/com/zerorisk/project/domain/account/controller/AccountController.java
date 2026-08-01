package com.zerorisk.project.domain.account.controller;

import com.zerorisk.project.domain.account.dto.AccountBalanceResponse;
import com.zerorisk.project.domain.account.dto.AccountResponse;
import com.zerorisk.project.domain.account.service.AccountService;
import com.zerorisk.project.global.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getMyAccounts(@CurrentUserId Long userId) {
        return accountService.getMyAccounts(userId);
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceResponse getBalance(
            @CurrentUserId Long userId,
            @PathVariable Long accountId
    ) {
        return accountService.getAccountBalance(userId, accountId);
    }
}
