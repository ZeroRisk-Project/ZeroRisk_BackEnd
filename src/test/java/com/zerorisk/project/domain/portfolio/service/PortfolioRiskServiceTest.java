package com.zerorisk.project.domain.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.portfolio.dto.PortfolioRiskResponse;
import com.zerorisk.project.domain.portfolio.entity.Holding;
import com.zerorisk.project.domain.portfolio.repository.HoldingRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.MarketIndexDailyPrice;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.entity.StockDailyPrice;
import com.zerorisk.project.domain.stock.repository.MarketIndexDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockDailyPriceRepository;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PortfolioRiskServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockDailyPriceRepository stockDailyPriceRepository;

    @Mock
    private MarketIndexDailyPriceRepository marketIndexDailyPriceRepository;

    @Mock
    private KisQuoteClient kisQuoteClient;

    private PortfolioRiskService service() {
        return new PortfolioRiskService(accountRepository, holdingRepository, stockRepository,
                stockDailyPriceRepository, marketIndexDailyPriceRepository, kisQuoteClient);
    }

    private Account account(Long id, Long userId) {
        Account account = Account.builder().userId(userId).accountType(AccountType.BASIC).competitionId(null).build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Stock stock(Long id, String code) {
        Stock stock = Stock.builder().code(code).standardCode("KR7" + code + "003").name(code).market(Market.KOSPI).build();
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }

    private Holding holding(Long accountId, Long stockId, long quantity) {
        return Holding.builder().accountId(accountId).stockId(stockId).quantity(quantity).averagePrice(new BigDecimal("10000")).build();
    }

    @DisplayName("보유 종목이 없으면 데이터 부족으로 응답한다")
    @Test
    void 보유_종목이_없으면_데이터_부족() {
        given(accountRepository.findById(1L)).willReturn(java.util.Optional.of(account(1L, 10L)));
        given(holdingRepository.findByAccountId(1L)).willReturn(List.of());

        PortfolioRiskResponse response = service().getRisk(10L, 1L);

        assertThat(response.available()).isFalse();
    }

    @DisplayName("겹치는 시세 이력이 20일 미만이면 데이터 부족으로 응답한다")
    @Test
    void 이력이_부족하면_데이터_부족() {
        given(accountRepository.findById(1L)).willReturn(java.util.Optional.of(account(1L, 10L)));
        given(holdingRepository.findByAccountId(1L)).willReturn(List.of(holding(1L, 100L, 10)));
        given(stockRepository.findAllById(List.of(100L))).willReturn(List.of(stock(100L, "005930")));
        given(kisQuoteClient.fetchQuote("005930"))
                .willReturn(new KisQuoteResponse.Output("70000", "0", "2", "0", "0", "0"));
        given(marketIndexDailyPriceRepository.findByMarketAndPriceDateBetweenOrderByPriceDateAsc(eq(Market.KOSPI), any(), any()))
                .willReturn(List.of());
        given(stockDailyPriceRepository.findByStockIdAndPriceDateBetweenOrderByPriceDateAsc(eq(100L), any(), any()))
                .willReturn(List.of());

        PortfolioRiskResponse response = service().getRisk(10L, 1L);

        assertThat(response.available()).isFalse();
    }

    // 종목 수익률이 매일 지수 수익률의 정확히 2배가 되도록 가격을 구성하면, 공분산 공식에 따라
    // 베타는 정확히 2.0이 나와야 한다(Cov(2r, r)/Var(r) = 2·Var(r)/Var(r) = 2). 지수 수익률은
    // +1%/-1%를 번갈아 반복해 평균 0, 분산을 손으로도 검산 가능하게 구성했다.
    @DisplayName("종목 수익률이 지수의 2배면 베타는 2.0으로 계산된다")
    @Test
    void 베타를_알려진_관계로_검증한다() {
        given(accountRepository.findById(1L)).willReturn(java.util.Optional.of(account(1L, 10L)));
        given(holdingRepository.findByAccountId(1L)).willReturn(List.of(holding(1L, 100L, 10)));
        given(stockRepository.findAllById(List.of(100L))).willReturn(List.of(stock(100L, "005930")));
        given(kisQuoteClient.fetchQuote("005930"))
                .willReturn(new KisQuoteResponse.Output("70000", "0", "2", "0", "0", "0"));

        LocalDate start = LocalDate.now().minusDays(30);
        List<MarketIndexDailyPrice> indexPrices = new ArrayList<>();
        List<StockDailyPrice> stockPrices = new ArrayList<>();
        BigDecimal indexPrice = new BigDecimal("100");
        BigDecimal stockPrice = new BigDecimal("100");
        for (int i = 0; i < 21; i++) {
            LocalDate date = start.plusDays(i);
            double indexReturn = i == 0 ? 0 : (i % 2 == 1 ? 0.01 : -0.01);
            if (i > 0) {
                indexPrice = indexPrice.multiply(BigDecimal.valueOf(1 + indexReturn));
                stockPrice = stockPrice.multiply(BigDecimal.valueOf(1 + 2 * indexReturn));
            }
            indexPrices.add(MarketIndexDailyPrice.builder().market(Market.KOSPI).priceDate(date).closeValue(indexPrice).build());
            stockPrices.add(StockDailyPrice.builder().stockId(100L).priceDate(date).closePrice(stockPrice).build());
        }

        given(marketIndexDailyPriceRepository.findByMarketAndPriceDateBetweenOrderByPriceDateAsc(eq(Market.KOSPI), any(), any()))
                .willReturn(indexPrices);
        given(stockDailyPriceRepository.findByStockIdAndPriceDateBetweenOrderByPriceDateAsc(eq(100L), any(), any()))
                .willReturn(stockPrices);

        PortfolioRiskResponse response = service().getRisk(10L, 1L);

        assertThat(response.available()).isTrue();
        assertThat(response.beta()).isEqualByComparingTo(new BigDecimal("2.00"));
    }
}
