package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.stock.cache.StockAliasCache;
import com.zerorisk.project.domain.stock.dto.StockSummaryResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class StockSearchServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockAliasCache stockAliasCache;

    private StockSearchService stockSearchService;

    @DisplayName("별칭에 매칭되면 해당 종목 하나만 반환")
    @Test
    void 별칭에_매칭되면_해당_종목_하나만_반환() {
        stockSearchService = new StockSearchService(stockRepository, stockAliasCache);
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        given(stockAliasCache.resolve("삼전")).willReturn(Optional.of("005930"));
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock));

        Page<StockSummaryResponse> result = stockSearchService.search("삼전", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("005930");
        verify(stockRepository, never()).search("삼전", PageRequest.of(0, 10));
    }

    @DisplayName("별칭에 매칭되지 않으면 코드 또는 이름으로 통합 검색")
    @Test
    void 별칭에_매칭되지_않으면_코드_또는_이름으로_통합_검색() {
        stockSearchService = new StockSearchService(stockRepository, stockAliasCache);
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        given(stockAliasCache.resolve("삼성")).willReturn(Optional.empty());
        given(stockRepository.search("삼성", pageable)).willReturn(new PageImpl<>(java.util.List.of(stock)));

        Page<StockSummaryResponse> result = stockSearchService.search("삼성", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("삼성전자");
    }
}