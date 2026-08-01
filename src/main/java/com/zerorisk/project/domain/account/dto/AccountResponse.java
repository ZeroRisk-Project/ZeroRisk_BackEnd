package com.zerorisk.project.domain.account.dto;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import java.math.BigDecimal;

public record AccountResponse(
        Long accountId,
        AccountType accountType,
        BigDecimal balance,
        Long competitionId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(), account.getAccountType(),
                account.getBalance(), account.getCompetitionId()
        );
    }
}
