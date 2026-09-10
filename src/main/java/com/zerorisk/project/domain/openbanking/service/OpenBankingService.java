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
import org.springframework.dao.DataIntegrityViolationException;
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
        if (userInfoResponse.res_list() == null || userInfoResponse.res_list().isEmpty()) {
            throw new OpenBankingException(OpenBankingErrorCode.NO_LINKED_ACCOUNT);
        }
        // 여러 계좌를 동의했더라도 이 서비스는 유저당 계좌 하나만 연동하므로(ACCOUNTS 유니크 제약과 동일한
        // 설계) 첫 번째 계좌만 사용한다 - 의도된 동작이며 나머지 계좌는 그냥 쓰지 않는다.
        var account = userInfoResponse.res_list().get(0);

        OpenBankingAuth auth = OpenBankingAuth.builder()
                .userId(userId)
                .bankName(account.bank_name())
                .accountNumMasked(account.account_num_masked())
                .fintechUseNum(account.fintech_use_num())
                .build();
        try {
            // 위의 isPresent() 체크와 이 저장 사이의 동시 요청 race를 유니크 제약(OPENBANKING_AUTHS.USER_ID)으로
            // 최종 방어한다. flush까지 강제해야 이 안에서 위반을 바로 잡아낼 수 있다.
            openBankingAuthRepository.saveAndFlush(auth);
        } catch (DataIntegrityViolationException e) {
            throw new OpenBankingException(OpenBankingErrorCode.ALREADY_AUTHENTICATED);
        }

        // 계좌 인증이 처음이면, 이 시점에 BASIC 계좌를 만들어준다.
        // ACCOUNTS의 유니크 제약으로 동시 생성(연습용 크레딧 등)이 걸리면 이미 있는 걸로 보고 무시한다.
        if (accountRepository.findByUserIdAndAccountType(userId, AccountType.BASIC).isEmpty()) {
            Account basicAccount = Account.builder()
                    .userId(userId)
                    .accountType(AccountType.BASIC)
                    .build();
            try {
                accountRepository.saveAndFlush(basicAccount);
            } catch (DataIntegrityViolationException e) {
                log.info("BASIC 계좌 동시 생성 감지 - userId: {}, 기존 계좌를 그대로 둡니다.", userId);
            }
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