package com.zerorisk.project.domain.openbanking.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.openbanking.dto.AuthenticateAccountResponse;
import com.zerorisk.project.domain.openbanking.dto.BalanceLimitResponse;
import com.zerorisk.project.domain.openbanking.dto.OpenBankingAuthResponse;
import com.zerorisk.project.domain.openbanking.entity.OpenBankingAuth;
import com.zerorisk.project.domain.openbanking.exception.OpenBankingErrorCode;
import com.zerorisk.project.domain.openbanking.exception.OpenBankingException;
import com.zerorisk.project.domain.openbanking.repository.OpenBankingAuthRepository;
import com.zerorisk.project.global.audit.UserActivityLogger;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpenBankingService {

    private final OpenBankingAuthRepository openBankingAuthRepository;
    private final AccountRepository accountRepository;
    private final OpenBankingClient openBankingClient;
    private final UserActivityLogger userActivityLogger;

    @Transactional
    public AuthenticateAccountResponse authenticateAccount(Long userId, String authorizationCode) {
        if (openBankingAuthRepository.findByUserId(userId).isPresent()) {
            throw new OpenBankingException(OpenBankingErrorCode.ALREADY_AUTHENTICATED);
        }

        var tokenResponse = openBankingClient.exchangeToken(authorizationCode);

        var userInfoResponse = openBankingClient.getUserInfo(tokenResponse.access_token(), tokenResponse.user_seq_no());
        var account = userInfoResponse.res_list().get(0);

        OpenBankingAuth auth = OpenBankingAuth.builder()
                .userId(userId)
                .bankName(account.bank_name())
                .accountNumMasked(account.account_num_masked())
                .fintechUseNum(account.fintech_use_num())
                .build();
        openBankingAuthRepository.save(auth);

        // 계좌 인증이 처음이면, 이 시점에 BASIC 계좌를 만들어준다
        if (accountRepository.findByUserIdAndAccountType(userId, AccountType.BASIC).isEmpty()) {
            Account basicAccount = Account.builder()
                    .userId(userId)
                    .accountType(AccountType.BASIC)
                    .build();
            accountRepository.save(basicAccount);
        }

        log.info("오픈뱅킹 계좌 인증 완료 - userId: {}, fintechUseNum: [REDACTED]", userId);
        userActivityLogger.log(userId, "OPENBANKING_AUTH", account.bank_name() + " 계좌 인증 완료");

        return new AuthenticateAccountResponse(account.bank_name(), account.account_num_masked());
    }

    public OpenBankingAuthResponse getMyAuth(Long userId) {
        OpenBankingAuth auth = openBankingAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new OpenBankingException(OpenBankingErrorCode.AUTH_NOT_FOUND));

        return new OpenBankingAuthResponse(auth.getBankName(), auth.getAccountNumMasked(), auth.getVerifiedAt());
    }

    public BalanceLimitResponse getAvailableChargeAmount(Long userId) {
        OpenBankingAuth auth = openBankingAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new OpenBankingException(OpenBankingErrorCode.AUTH_NOT_FOUND));

        BigDecimal availableAmount = calculateAvailableAmount(auth);

        log.info("충전 가능 한도 계산 완료 - userId: {}, availableAmount: {}", userId, availableAmount);

        return new BalanceLimitResponse(availableAmount);
    }

    @Transactional
    public void chargeSeedMoney(Long userId, BigDecimal requestedAmount) {
        OpenBankingAuth auth = openBankingAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new OpenBankingException(OpenBankingErrorCode.AUTH_NOT_FOUND));

        BigDecimal availableAmount = calculateAvailableAmount(auth);

        if (requestedAmount.compareTo(availableAmount) > 0) {
            throw new OpenBankingException(OpenBankingErrorCode.CHARGE_LIMIT_EXCEEDED);
        }

        Account account = accountRepository.findBasicAccountByUserIdForUpdate(userId)
                .orElseThrow(() -> new OpenBankingException(OpenBankingErrorCode.AUTH_NOT_FOUND));

        account.addSeedMoney(requestedAmount);
        auth.addReceivedPoints(requestedAmount);

        log.info("시드머니 충전 완료 - userId: {}, chargedAmount: {}", userId, requestedAmount);
        userActivityLogger.log(userId, "CHARGE", requestedAmount + "원 충전");
    }

    private BigDecimal calculateAvailableAmount(OpenBankingAuth auth) {
        var balanceResponse = openBankingClient.inquireBalance("mock_access_token", auth.getFintechUseNum());
        BigDecimal actualBalance = new BigDecimal(balanceResponse.balance_amt());

        BigDecimal remaining = actualBalance.subtract(auth.getTotalReceivedPoints());

        return remaining.max(BigDecimal.ZERO);
    }
}