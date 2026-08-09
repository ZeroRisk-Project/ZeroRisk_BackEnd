package com.zerorisk.project.domain.stock.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisApprovalResponse(
        @JsonProperty("approval_key") String approvalKey) {
}