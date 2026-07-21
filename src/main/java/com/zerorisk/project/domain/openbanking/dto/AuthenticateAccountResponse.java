package com.zerorisk.project.domain.openbanking.dto;

public record AuthenticateAccountResponse(
        String bankName,
        String accountNumMasked) {
}