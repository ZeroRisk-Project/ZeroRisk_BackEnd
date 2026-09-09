package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.stock.cache.StockAliasCache;
import com.zerorisk.project.domain.stock.client.kis.KisStockMasterClient;
import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class StockMasterSyncServiceTest {

    @Mock
    private KisStockMasterClient kisStockMasterClient;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockAliasCache stockAliasCache;

    @Mock
    private PlatformTransactionManager transactionManager;

    @DisplayName("한 종목 저장이 실패해도 나머지 종목은 정상 저장된다")
    @Test
    void 한_종목_저장_실패가_전체_동기화를_막지_않는다() {
        given(transactionManager.getTransaction(any())).willReturn(org.mockito.Mockito.mock(TransactionStatus.class));

        StockMasterSyncService service = new StockMasterSyncService(
                kisStockMasterClient, stockRepository, stockAliasCache, transactionManager);

        given(kisStockMasterClient.fetchAll()).willReturn(List.of(
                new StockMasterRow("000001", "KR001", "정상종목", Market.KOSPI),
                new StockMasterRow("000002", "KR002", "실패종목", Market.KOSPI)));

        given(stockRepository.findByCode("000001")).willReturn(Optional.empty());
        given(stockRepository.findByCode("000002")).willReturn(Optional.empty());
        doThrow(new RuntimeException("DB 오류"))
                .when(stockRepository)
                .save(org.mockito.ArgumentMatchers.argThat(
                        stock -> stock != null && "000002".equals(stock.getCode())));
        given(stockRepository.findAll()).willReturn(List.of());

        assertThatCode(service::sync).doesNotThrowAnyException();

        verify(stockRepository).save(org.mockito.ArgumentMatchers.argThat(
                stock -> stock != null && "000001".equals(stock.getCode())));
    }

    @DisplayName("마스터 데이터가 비어 있으면 동기화를 건너뛴다")
    @Test
    void 마스터_데이터가_비어있으면_동기화를_건너뛴다() {
        StockMasterSyncService service = new StockMasterSyncService(
                kisStockMasterClient, stockRepository, stockAliasCache, transactionManager);

        given(kisStockMasterClient.fetchAll()).willReturn(List.of());

        assertThatCode(service::sync).doesNotThrowAnyException();

        verify(stockRepository, never()).findAll();
        verify(stockAliasCache, never()).reload();
    }
}
