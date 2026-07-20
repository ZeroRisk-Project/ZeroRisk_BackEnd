package com.zerorisk.project.domain.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisQuoteResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") Output output) {

    public record Output(
            @JsonProperty("stck_prpr") String currentPrice,
            @JsonProperty("prdy_vrss") String changeAmount,
            @JsonProperty("prdy_vrss_sign") String changeSign,
            @JsonProperty("prdy_ctrt") String changeRate,
            @JsonProperty("w52_hgpr") String week52High,
            @JsonProperty("w52_lwpr") String week52Low) {
    }
}