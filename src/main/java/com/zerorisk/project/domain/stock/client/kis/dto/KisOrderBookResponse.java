package com.zerorisk.project.domain.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisOrderBookResponse(
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output1") Output1 output1) {

    // 매도호가 1~10(askp), 매수호가 1~10(bidp)과 각 호가의 잔량(*_rsqn) - 숫자가 작을수록
    // 현재가에 가까운 호가다(askp1이 가장 낮은 매도호가, bidp1이 가장 높은 매수호가).
    public record Output1(
            @JsonProperty("askp1") String askPrice1,
            @JsonProperty("askp2") String askPrice2,
            @JsonProperty("askp3") String askPrice3,
            @JsonProperty("askp4") String askPrice4,
            @JsonProperty("askp5") String askPrice5,
            @JsonProperty("askp6") String askPrice6,
            @JsonProperty("askp7") String askPrice7,
            @JsonProperty("askp8") String askPrice8,
            @JsonProperty("askp9") String askPrice9,
            @JsonProperty("askp10") String askPrice10,
            @JsonProperty("bidp1") String bidPrice1,
            @JsonProperty("bidp2") String bidPrice2,
            @JsonProperty("bidp3") String bidPrice3,
            @JsonProperty("bidp4") String bidPrice4,
            @JsonProperty("bidp5") String bidPrice5,
            @JsonProperty("bidp6") String bidPrice6,
            @JsonProperty("bidp7") String bidPrice7,
            @JsonProperty("bidp8") String bidPrice8,
            @JsonProperty("bidp9") String bidPrice9,
            @JsonProperty("bidp10") String bidPrice10,
            @JsonProperty("askp_rsqn1") String askQuantity1,
            @JsonProperty("askp_rsqn2") String askQuantity2,
            @JsonProperty("askp_rsqn3") String askQuantity3,
            @JsonProperty("askp_rsqn4") String askQuantity4,
            @JsonProperty("askp_rsqn5") String askQuantity5,
            @JsonProperty("askp_rsqn6") String askQuantity6,
            @JsonProperty("askp_rsqn7") String askQuantity7,
            @JsonProperty("askp_rsqn8") String askQuantity8,
            @JsonProperty("askp_rsqn9") String askQuantity9,
            @JsonProperty("askp_rsqn10") String askQuantity10,
            @JsonProperty("bidp_rsqn1") String bidQuantity1,
            @JsonProperty("bidp_rsqn2") String bidQuantity2,
            @JsonProperty("bidp_rsqn3") String bidQuantity3,
            @JsonProperty("bidp_rsqn4") String bidQuantity4,
            @JsonProperty("bidp_rsqn5") String bidQuantity5,
            @JsonProperty("bidp_rsqn6") String bidQuantity6,
            @JsonProperty("bidp_rsqn7") String bidQuantity7,
            @JsonProperty("bidp_rsqn8") String bidQuantity8,
            @JsonProperty("bidp_rsqn9") String bidQuantity9,
            @JsonProperty("bidp_rsqn10") String bidQuantity10,
            @JsonProperty("total_askp_rsqn") String totalAskQuantity,
            @JsonProperty("total_bidp_rsqn") String totalBidQuantity) {
    }
}
