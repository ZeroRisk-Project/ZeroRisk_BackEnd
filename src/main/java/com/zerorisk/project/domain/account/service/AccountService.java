package com.zerorisk.project.domain.account.service;

import com.zerorisk.project.domain.account.dto.AccountBalanceResponse;
import com.zerorisk.project.domain.account.dto.AccountResponse;
import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountResponse> getMyAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountBalanceResponse getAccountBalance(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getUserId().equals(userId)) {
            throw new AccountException(AccountErrorCode.ACCESS_DENIED);
        }

        return new AccountBalanceResponse(account.getBalance());
    }
}
