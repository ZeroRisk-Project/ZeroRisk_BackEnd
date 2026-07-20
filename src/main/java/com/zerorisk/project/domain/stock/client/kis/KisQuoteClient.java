package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;

public interface KisQuoteClient {
    KisQuoteResponse.Output fetchQuote(String code);
}