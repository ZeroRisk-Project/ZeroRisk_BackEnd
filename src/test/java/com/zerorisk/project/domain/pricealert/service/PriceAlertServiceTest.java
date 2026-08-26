package com.zerorisk.project.domain.pricealert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.pricealert.dto.PriceAlertCreateRequest;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertResponse;
import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.entity.PriceAlertDirection;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertException;
import com.zerorisk.project.domain.pricealert.repository.PriceAlertRepository;
import com.zerorisk.project.domain.stock.entity.Market;
import com.zerorisk.project.domain.stock.entity.Stock;
import com.zerorisk.project.domain.stock.repository.StockRepository;
import com.zerorisk.project.global.exception.StockNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private StockRepository stockRepository;

    private PriceAlertService priceAlertService;

    private Stock stock() {
        Stock stock = Stock.builder()
                .code("005930")
                .standardCode("KR7005930003")
                .name("삼성전자")
                .market(Market.KOSPI)
                .build();
        ReflectionTestUtils.setField(stock, "id", 1L);
        return stock;
    }

    private PriceAlert alert(Long userId) {
        return PriceAlert.builder()
                .userId(userId)
                .stockId(1L)
                .targetPrice(new BigDecimal("80000"))
                .direction(PriceAlertDirection.ABOVE)
                .build();
    }

    @DisplayName("목표가 알림을 등록")
    @Test
    void 목표가_알림을_등록() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        given(stockRepository.findByCode("005930")).willReturn(Optional.of(stock()));

        PriceAlertCreateRequest request = new PriceAlertCreateRequest(
                "005930", new BigDecimal("80000"), PriceAlertDirection.ABOVE);
        PriceAlertResponse response = priceAlertService.createAlert(1L, request);

        assertThat(response.stockCode()).isEqualTo("005930");
        assertThat(response.targetPrice()).isEqualByComparingTo("80000");
        assertThat(response.direction()).isEqualTo(PriceAlertDirection.ABOVE);
    }

    @DisplayName("존재하지 않는 종목으로 알림을 등록할 시 예외 발생")
    @Test
    void 존재하지_않는_종목으로_알림을_등록할_시_예외_발생() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        given(stockRepository.findByCode("999999")).willReturn(Optional.empty());

        PriceAlertCreateRequest request = new PriceAlertCreateRequest(
                "999999", new BigDecimal("80000"), PriceAlertDirection.ABOVE);

        assertThatThrownBy(() -> priceAlertService.createAlert(1L, request))
                .isInstanceOf(StockNotFoundException.class);
    }

    @DisplayName("사용자의 목표가 알림 목록을 조회")
    @Test
    void 사용자의_목표가_알림_목록을_조회() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        given(priceAlertRepository.findByUserId(1L)).willReturn(List.of(alert(1L)));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));

        List<PriceAlertResponse> response = priceAlertService.getAlerts(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).stockCode()).isEqualTo("005930");
    }

    @DisplayName("목표가 알림을 삭제")
    @Test
    void 목표가_알림을_삭제() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        PriceAlert alert = alert(1L);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.of(alert));

        priceAlertService.deleteAlert(1L, 20L);

        verify(priceAlertRepository).delete(alert);
    }

    @DisplayName("다른 사용자의 목표가 알림을 삭제할 시 예외 발생")
    @Test
    void 다른_사용자의_목표가_알림을_삭제할_시_예외_발생() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.of(alert(2L)));

        assertThatThrownBy(() -> priceAlertService.deleteAlert(1L, 20L))
                .isInstanceOf(PriceAlertException.class);
    }

    @DisplayName("존재하지 않는 목표가 알림을 삭제할 시 예외 발생")
    @Test
    void 존재하지_않는_목표가_알림을_삭제할_시_예외_발생() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> priceAlertService.deleteAlert(1L, 20L))
                .isInstanceOf(PriceAlertException.class);
    }
}