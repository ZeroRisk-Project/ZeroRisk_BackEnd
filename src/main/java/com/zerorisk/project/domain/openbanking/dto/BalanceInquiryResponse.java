package com.zerorisk.project.domain.openbanking.dto;

public record BalanceInquiryResponse(
        String api_tran_id,
        String api_tran_dtm,
        String bank_tran_id,
        String bank_rsp_code,
        String bank_rsp_message,
        String bank_name,
        String fintech_use_num,
        String balance_amt,
        String available_amt,
        String account_type,
        String product_name) {
}