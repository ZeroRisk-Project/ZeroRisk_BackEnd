package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisChartClient;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisDailyChartResponse;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockDailyPrice;
import com.zerorisk.project.domain.stock.repository.StockDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class StockDailyPriceServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockDailyPriceRepository stockDailyPriceRepository;

    @Mock
    private KisChartClient kisChartClient;

    @Mock
    private KisQuoteClient kisQuoteClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private StockDailyPriceService service() {
        given(transactionManager.getTransaction(any())).willReturn(mock(TransactionStatus.class));
        return new StockDailyPriceService(
                holdingRepository, stockRepository, stockDailyPriceRepository,
                kisChartClient, kisQuoteClient, transactionManager);
    }

    private Stock stock(Long id, String code) {
        Stock stock = Stock.builder().code(code).standardCode("KR7" + code + "003").name(code).market(Market.KOSPI).build();
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }

    @DisplayName("이력이 없는 종목은 과거 100일치를 백필한다")
    @Test
    void 이력이_없는_종목은_백필한다() {
        StockDailyPriceService service = service();
        given(holdingRepository.findDistinctStockIds()).willReturn(List.of(1L));
        given(stockDailyPriceRepository.existsByStockIdAndPriceDate(eq(1L), any())).willReturn(false);
        given(stockRepository.findById(1L)).willReturn(Optional.of(stock(1L, "005930")));
        given(stockDailyPriceRepository.existsByStockId(1L)).willReturn(false);
        given(kisChartClient.fetchDailyChart(eq("005930"), eq("D"), any(), any())).willReturn(List.of(
                new KisDailyChartResponse.Candle("20260908", "70000", "71000", "69000", "70500", "1000"),
                new KisDailyChartResponse.Candle("20260909", "70500", "72000", "70000", "71500", "1200")));

        service.createDailyPrices();

        verify(stockDailyPriceRepository, org.mockito.Mockito.times(2)).save(any(StockDailyPrice.class));
        verify(kisQuoteClient, never()).fetchQuote(any());
    }

    @DisplayName("이력이 있는 종목은 오늘자 시세 1건만 추가한다")
    @Test
    void 이력이_있는_종목은_오늘자만_추가한다() {
        StockDailyPriceService service = service();
        given(holdingRepository.findDistinctStockIds()).willReturn(List.of(1L));
        given(stockDailyPriceRepository.existsByStockIdAndPriceDate(eq(1L), any())).willReturn(false);
        given(stockRepository.findById(1L)).willReturn(Optional.of(stock(1L, "005930")));
        given(stockDailyPriceRepository.existsByStockId(1L)).willReturn(true);
        given(kisQuoteClient.fetchQuote("005930"))
                .willReturn(new KisQuoteResponse.Output("71500", "1000", "2", "1.42", "80000", "60000"));

        service.createDailyPrices();

        verify(stockDailyPriceRepository).save(any(StockDailyPrice.class));
        verify(kisChartClient, never()).fetchDailyChart(any(), any(), any(), any());
    }

    @DisplayName("이미 오늘자 데이터가 있는 종목은 건너뛴다")
    @Test
    void 오늘자_데이터가_있으면_건너뛴다() {
        StockDailyPriceService service = new StockDailyPriceService(
                holdingRepository, stockRepository, stockDailyPriceRepository,
                kisChartClient, kisQuoteClient, transactionManager);
        given(holdingRepository.findDistinctStockIds()).willReturn(List.of(1L));
        given(stockDailyPriceRepository.existsByStockIdAndPriceDate(eq(1L), any(LocalDate.class))).willReturn(true);

        service.createDailyPrices();

        verify(stockRepository, never()).findById(any());
    }

    @DisplayName("한 종목 조회가 실패해도 나머지 종목은 계속 처리한다")
    @Test
    void 한_종목_실패해도_나머지는_계속_처리한다() {
        StockDailyPriceService service = service();
        given(holdingRepository.findDistinctStockIds()).willReturn(List.of(1L, 2L));
        given(stockDailyPriceRepository.existsByStockIdAndPriceDate(any(), any())).willReturn(false);
        given(stockRepository.findById(1L)).willReturn(Optional.of(stock(1L, "005930")));
        given(stockRepository.findById(2L)).willReturn(Optional.of(stock(2L, "000660")));
        given(stockDailyPriceRepository.existsByStockId(1L)).willReturn(true);
        given(stockDailyPriceRepository.existsByStockId(2L)).willReturn(true);
        given(kisQuoteClient.fetchQuote("005930")).willThrow(new RuntimeException("KIS 오류"));
        given(kisQuoteClient.fetchQuote("000660"))
                .willReturn(new KisQuoteResponse.Output("150000", "1000", "2", "0.67", "180000", "100000"));

        assertThatCode(service::createDailyPrices).doesNotThrowAnyException();

        verify(stockDailyPriceRepository).save(any(StockDailyPrice.class));
    }
}
