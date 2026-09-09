package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisIndexClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;
import com.zerorisk.project.domain.stock.dto.MarketIndexResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndexService {

    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");
    private static final String KOSPI_INDEX_CODE = "0001";
    private static final String KOSDAQ_INDEX_CODE = "1001";

    private final KisIndexClient kisIndexClient;

    public List<MarketIndexResponse> getIndices() {
        return Stream.of(fetch(Market.KOSPI, KOSPI_INDEX_CODE), fetch(Market.KOSDAQ, KOSDAQ_INDEX_CODE))
                .filter(Objects::nonNull)
                .toList();
    }

    private MarketIndexResponse fetch(Market market, String indexCode) {
        try {
            KisIndexResponse.Output output = kisIndexClient.fetchIndex(indexCode);
            BigDecimal changeAmount = new BigDecimal(output.changeAmount());
            if (NEGATIVE_SIGNS.contains(output.changeSign())) {
                changeAmount = changeAmount.negate();
            }
            return new MarketIndexResponse(
                    market,
                    new BigDecimal(output.currentIndex()),
                    changeAmount,
                    new BigDecimal(output.changeRate()));
        } catch (Exception e) {
            log.warn("지수 조회 실패: market={}", market, e);
            return null;
        }
    }
}
