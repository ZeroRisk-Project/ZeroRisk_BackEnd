package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import java.util.List;

public interface KisRankingClient {
    List<KisRankingResponse.Output> fetchVolumeRanking();
}