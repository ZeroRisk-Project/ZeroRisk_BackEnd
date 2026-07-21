package com.zerorisk.project.domain.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisRankingResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") List<Output> output) {

    public record Output(
            @JsonProperty("mksc_shrn_iscd") String code,
            @JsonProperty("hts_kor_isnm") String name,
            @JsonProperty("stck_prpr") String currentPrice,
            @JsonProperty("prdy_vrss") String changeAmount,
            @JsonProperty("prdy_vrss_sign") String changeSign,
            @JsonProperty("prdy_ctrt") String changeRate,
            @JsonProperty("acml_vol") String volume) {
    }
}