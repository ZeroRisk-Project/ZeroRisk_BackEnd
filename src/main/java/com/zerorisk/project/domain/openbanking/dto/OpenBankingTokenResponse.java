package com.zerorisk.project.domain.openbanking.dto;

public record OpenBankingTokenResponse(
        String access_token,
        String token_type,
        String expires_in,
        String scope,
        String user_seq_no) {
}