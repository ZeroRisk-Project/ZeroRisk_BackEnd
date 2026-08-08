package com.zerorisk.project.domain.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisMinuteChartResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output2") List<Candle> candles) {

    public record Candle(
            @JsonProperty("stck_bsop_date") String date,
            @JsonProperty("stck_cntg_hour") String time,
            @JsonProperty("stck_oprc") String open,
            @JsonProperty("stck_hgpr") String high,
            @JsonProperty("stck_lwpr") String low,
            @JsonProperty("stck_prpr") String close,
            @JsonProperty("cntg_vol") String volume) {
    }
}