package com.zerorisk.project.domain.openbanking.dto;

import java.util.List;

public record AccountRegisterResponse(
        String api_tran_id,
        String api_tran_dtm,
        String bank_tran_id,
        String bank_tran_date,
        String bank_rsp_code,
        String bank_rsp_message,
        List<RegisteredAccount> res_list) {
    public record RegisteredAccount(
            String fintech_use_num,
            String account_alias,
            String bank_code_std,
            String bank_name,
            String account_num_masked,
            String account_holder_name,
            String account_type) {
    }
}