package com.zerorisk.project.domain.openbanking.service;

import com.zerorisk.project.domain.openbanking.dto.UserInfoResponse;
import com.zerorisk.project.domain.openbanking.dto.BalanceInquiryResponse;
import com.zerorisk.project.domain.openbanking.dto.OpenBankingTokenResponse;

public interface OpenBankingClient {
    OpenBankingTokenResponse exchangeToken(String authorizationCode);

    UserInfoResponse getUserInfo(String accessToken, String userSeqNo);

    BalanceInquiryResponse inquireBalance(String accessToken, String fintechUseNum);
}