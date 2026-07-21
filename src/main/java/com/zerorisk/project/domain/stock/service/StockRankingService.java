package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisRankingClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import com.zerorisk.project.domain.stock.dto.RankingType;
import com.zerorisk.project.domain.stock.dto.StockRankingResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockRankingService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");

    private final KisRankingClient kisRankingClient;

    public List<StockRankingResponse> getRankings(RankingType type, int count) {
        List<StockRankingResponse> rankings = kisRankingClient.fetchVolumeRanking().stream()
                .map(this::toResponse)
                .toList();

        Comparator<StockRankingResponse> comparator = switch (type) {
            case VOLUME -> Comparator.comparing(StockRankingResponse::volume).reversed();
            case RISE -> Comparator.comparing(StockRankingResponse::changeRate).reversed();
            case FALL -> Comparator.comparing(StockRankingResponse::changeRate);
        };

        return rankings.stream()
                .sorted(comparator)
                .limit(count)
                .toList();
    }

    private StockRankingResponse toResponse(KisRankingResponse.Output output) {
        long changeAmount = Long.parseLong(output.changeAmount());
        if (NEGATIVE_SIGNS.contains(output.changeSign())) {
            changeAmount = -changeAmount;
        }

        return new StockRankingResponse(
                output.code(),
                output.name(),
                Long.parseLong(output.currentPrice()),
                changeAmount,
                new BigDecimal(output.changeRate()),
                Long.parseLong(output.volume()));
    }
}