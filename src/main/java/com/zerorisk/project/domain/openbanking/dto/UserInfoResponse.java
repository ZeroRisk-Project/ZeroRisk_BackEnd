package com.zerorisk.project.domain.openbanking.dto;

import java.util.List;

public record UserInfoResponse(
        String api_tran_id,
        String api_tran_dtm,
        String rsp_code,
        String rsp_message,
        String user_seq_no,
        String user_ci,
        String user_name,
        String res_cnt,
        List<RegisteredAccount> res_list) {
    public record RegisteredAccount(
            String fintech_use_num,
            String account_alias,
            String bank_code_std,
            String bank_code_sub,
            String bank_name,
            String savings_bank_name,
            String account_num_masked,
            String account_seq,
            String account_holder_name,
            String account_holder_type,
            String account_type,
            String inquiry_agree_yn,
            String inquiry_agree_dtime,
            String transfer_agree_yn,
            String transfer_agree_dtime) {
    }
}