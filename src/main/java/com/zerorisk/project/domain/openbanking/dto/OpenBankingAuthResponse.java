package com.zerorisk.project.domain.openbanking.dto;

import java.time.LocalDateTime;

public record OpenBankingAuthResponse(
        String bankName,
        String accountNumMasked,
        LocalDateTime verifiedAt
) {
}
