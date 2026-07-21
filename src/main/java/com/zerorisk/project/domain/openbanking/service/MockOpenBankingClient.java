package com.zerorisk.project.domain.openbanking.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

import com.zerorisk.project.domain.openbanking.dto.UserInfoResponse;
import com.zerorisk.project.domain.openbanking.dto.BalanceInquiryResponse;
import com.zerorisk.project.domain.openbanking.dto.OpenBankingTokenResponse;

/**
 * 실제 금융결제원 오픈뱅킹 API는 이용적합성 승인(6~12개월, 심사비용 발생)이
 * 필요하여 포트폴리오 프로젝트 범위에서는 연동이 불가능합니다.
 * 실제 API 스펙(공개 명세서 기준)에 맞춘 Mock 구현체로 대체합니다.
 */
@Component
public class MockOpenBankingClient implements OpenBankingClient {

    private final SecureRandom random = new SecureRandom();

    @Override
    public OpenBankingTokenResponse exchangeToken(String authorizationCode) {
        return new OpenBankingTokenResponse(
                "mock_access_token_" + random.nextInt(1_000_000),
                "bearer",
                "7776000",
                "login inquiry transfer",
                "mock_user_seq_" + random.nextInt(100_000));
    }

    @Override
    public UserInfoResponse getUserInfo(String accessToken, String userSeqNo) {
        String fintechUseNum = generateFintechUseNum();
        String tranId = generateTranId();

        var account = new UserInfoResponse.RegisteredAccount(
                fintechUseNum,
                "모의계좌",
                "097",
                "0000000",
                "테스트뱅크",
                "",
                "110***1234",
                "1",
                "박종권",
                "P",
                "1",
                "Y",
                nowFormatted(),
                "N",
                "");

        return new UserInfoResponse(
                tranId,
                nowFormatted(),
                "A0000",
                "",
                userSeqNo,
                "mock_ci_" + random.nextInt(1_000_000),
                "박종권",
                "1",
                List.of(account));
    }

    @Override
    public BalanceInquiryResponse inquireBalance(String accessToken, String fintechUseNum) {
        String tranId = generateTranId();
        BigDecimal mockBalance = BigDecimal.valueOf(500_000 + random.nextInt(9_500_000));

        return new BalanceInquiryResponse(
                tranId,
                nowFormatted(),
                tranId,
                "A0000",
                "성공",
                "테스트뱅크",
                fintechUseNum,
                mockBalance.toPlainString(),
                mockBalance.toPlainString(),
                "1",
                "테스트예금");
    }

    private String generateFintechUseNum() {
        return String.valueOf(100_000_000_000L + random.nextInt(Integer.MAX_VALUE));
    }

    private String generateTranId() {
        return "M" + System.currentTimeMillis();
    }

    private String nowFormatted() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String nowDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}