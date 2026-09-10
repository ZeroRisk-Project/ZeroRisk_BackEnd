package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisIndexClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisIndexResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.MarketIndexDailyPrice;
import com.zerorisk.project.domain.stock.repository.MarketIndexDailyPriceRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class MarketIndexDailyPriceServiceTest {

    @Mock
    private KisIndexClient kisIndexClient;

    @Mock
    private MarketIndexDailyPriceRepository marketIndexDailyPriceRepository;

    private MarketIndexDailyPriceService service;

    @DisplayName("코스피/코스닥 둘 다 오늘자 레코드가 없으면 각각 저장한다")
    @Test
    void 둘_다_없으면_각각_저장한다() {
        service = new MarketIndexDailyPriceService(kisIndexClient, marketIndexDailyPriceRepository);
        given(marketIndexDailyPriceRepository.existsByMarketAndPriceDate(any(), any(LocalDate.class))).willReturn(false);
        given(kisIndexClient.fetchIndex(eq("0001")))
                .willReturn(new KisIndexResponse.Output("2682.43", "31.55", "2", "1.19"));
        given(kisIndexClient.fetchIndex(eq("1001")))
                .willReturn(new KisIndexResponse.Output("858.75", "4.12", "2", "0.48"));

        service.createDailyPrices();

        ArgumentCaptor<MarketIndexDailyPrice> captor = ArgumentCaptor.forClass(MarketIndexDailyPrice.class);
        Mockito.verify(marketIndexDailyPriceRepository, Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(MarketIndexDailyPrice::getMarket)
                .containsExactlyInAnyOrder(Market.KOSPI, Market.KOSDAQ);
        assertThat(captor.getAllValues().get(0).getCloseValue()).isNotNull();
    }

    @DisplayName("이미 오늘자 레코드가 있는 시장은 건너뛴다")
    @Test
    void 오늘자_레코드가_있으면_건너뛴다() {
        service = new MarketIndexDailyPriceService(kisIndexClient, marketIndexDailyPriceRepository);
        given(marketIndexDailyPriceRepository.existsByMarketAndPriceDate(eq(Market.KOSPI), any(LocalDate.class))).willReturn(true);
        given(marketIndexDailyPriceRepository.existsByMarketAndPriceDate(eq(Market.KOSDAQ), any(LocalDate.class))).willReturn(false);
        given(kisIndexClient.fetchIndex(eq("1001")))
                .willReturn(new KisIndexResponse.Output("858.75", "4.12", "2", "0.48"));

        service.createDailyPrices();

        Mockito.verify(kisIndexClient, Mockito.never()).fetchIndex(eq("0001"));
        Mockito.verify(marketIndexDailyPriceRepository).save(any());
    }

    @DisplayName("한 시장 조회가 실패해도 나머지 시장은 저장한다")
    @Test
    void 한_시장_실패해도_나머지는_저장한다() {
        service = new MarketIndexDailyPriceService(kisIndexClient, marketIndexDailyPriceRepository);
        given(marketIndexDailyPriceRepository.existsByMarketAndPriceDate(any(), any(LocalDate.class))).willReturn(false);
        given(kisIndexClient.fetchIndex(eq("0001"))).willThrow(new RuntimeException("KIS 오류"));
        given(kisIndexClient.fetchIndex(eq("1001")))
                .willReturn(new KisIndexResponse.Output("858.75", "4.12", "2", "0.48"));

        service.createDailyPrices();

        Mockito.verify(marketIndexDailyPriceRepository).save(any());
    }
}
