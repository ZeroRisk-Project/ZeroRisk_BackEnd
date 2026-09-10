package com.zerorisk.project.domain.stock.service;

import com.zerorisk.project.domain.stock.client.kis.KisIndexClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.MarketIndexDailyPrice;
import com.zerorisk.project.domain.stock.repository.MarketIndexDailyPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexDailyPriceService {

    private static final String KOSPI_INDEX_CODE = "0001";
    private static final String KOSDAQ_INDEX_CODE = "1001";

    private final KisIndexClient kisIndexClient;
    private final MarketIndexDailyPriceRepository marketIndexDailyPriceRepository;

    @Transactional
    public void createDailyPrices() {
        LocalDate today = LocalDate.now();
        int created = 0;
        created += saveIfMissing(Market.KOSPI, KOSPI_INDEX_CODE, today) ? 1 : 0;
        created += saveIfMissing(Market.KOSDAQ, KOSDAQ_INDEX_CODE, today) ? 1 : 0;
        log.info("지수 일별 종가 저장 배치 완료: {}건", created);
    }

    private boolean saveIfMissing(Market market, String indexCode, LocalDate today) {
        if (marketIndexDailyPriceRepository.existsByMarketAndPriceDate(market, today)) {
            return false;
        }
        try {
            KisIndexResponse.Output output = kisIndexClient.fetchIndex(indexCode);
            marketIndexDailyPriceRepository.save(MarketIndexDailyPrice.builder()
                    .market(market)
                    .priceDate(today)
                    .closeValue(new BigDecimal(output.currentIndex()))
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("지수 일별 종가 저장 실패: market={}", market, e);
            return false;
        }
    }
}
