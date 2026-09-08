package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisRankingClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import com.zerorisk.project.domain.stock.dto.RankingType;
import com.zerorisk.project.domain.stock.dto.StockRankingResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRankingService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");

    // KIS 거래량순위(FHPST01710000)는 한 번 호출에 시장 구분당 상위 30건만 내려주고
    // 페이징 파라미터가 없다. "0000"(전체)만으로는 두 시장을 합친 상위 30건만 보이므로,
    // 코스닥(1001)을 별도로 한 번 더 호출해 코스닥 상위권이 전체 랭킹에 가려지지 않게 합친다.
    // ("0001" 등 코스피 전용 코드는 이 TR에서 500을 반환해 지원되지 않는 것으로 확인됨.)
    private static final List<String> MARKET_CODES = List.of("0000", "1001");

    private final KisRankingClient kisRankingClient;

    public List<StockRankingResponse> getRankings(RankingType type, int count) {
        Map<String, StockRankingResponse> byCode = new LinkedHashMap<>();
        for (String marketCode : MARKET_CODES) {
            try {
                for (KisRankingResponse.Output output : kisRankingClient.fetchVolumeRanking(marketCode)) {
                    StockRankingResponse response = toResponse(output);
                    byCode.putIfAbsent(response.code(), response);
                }
            } catch (Exception e) {
                log.warn("거래량순위 조회 실패: marketCode={}", marketCode, e);
            }
        }
        List<StockRankingResponse> rankings = List.copyOf(byCode.values());

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