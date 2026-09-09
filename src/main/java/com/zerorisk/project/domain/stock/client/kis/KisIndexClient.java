package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;

public interface KisIndexClient {
    KisIndexResponse.Output fetchIndex(String indexCode);
}
