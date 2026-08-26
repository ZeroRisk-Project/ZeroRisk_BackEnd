package com.zerorisk.project.domain.pricealert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.notification.entity.NotificationType;
import com.zerorisk.project.domain.notification.service.NotificationService;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertCreateRequest;
import com.zerorisk.project.domain.pricealert.dto.PriceAlertResponse;
import com.zerorisk.project.domain.pricealert.entity.PriceAlert;
import com.zerorisk.project.domain.pricealert.entity.PriceAlertDirection;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertException;
import com.zerorisk.project.domain.pricealert.repository.PriceAlertRepository;
import com.zerorisk.project.domain.stock.client.kis.KisQuoteClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisQuoteResponse;
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

    @Mock
    private KisQuoteClient kisQuoteClient;

    @Mock
    private NotificationService notificationService;

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
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
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
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        given(stockRepository.findByCode("999999")).willReturn(Optional.empty());

        PriceAlertCreateRequest request = new PriceAlertCreateRequest(
                "999999", new BigDecimal("80000"), PriceAlertDirection.ABOVE);

        assertThatThrownBy(() -> priceAlertService.createAlert(1L, request))
                .isInstanceOf(StockNotFoundException.class);
    }

    @DisplayName("사용자의 목표가 알림 목록을 조회")
    @Test
    void 사용자의_목표가_알림_목록을_조회() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        given(priceAlertRepository.findByUserId(1L)).willReturn(List.of(alert(1L)));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));

        List<PriceAlertResponse> response = priceAlertService.getAlerts(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).stockCode()).isEqualTo("005930");
    }

    @DisplayName("목표가 알림을 삭제")
    @Test
    void 목표가_알림을_삭제() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        PriceAlert alert = alert(1L);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.of(alert));

        priceAlertService.deleteAlert(1L, 20L);

        verify(priceAlertRepository).delete(alert);
    }

    @DisplayName("다른 사용자의 목표가 알림을 삭제할 시 예외 발생")
    @Test
    void 다른_사용자의_목표가_알림을_삭제할_시_예외_발생() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.of(alert(2L)));

        assertThatThrownBy(() -> priceAlertService.deleteAlert(1L, 20L))
                .isInstanceOf(PriceAlertException.class);
    }

    @DisplayName("존재하지 않는 목표가 알림을 삭제할 시 예외 발생")
    @Test
    void 존재하지_않는_목표가_알림을_삭제할_시_예외_발생() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        given(priceAlertRepository.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> priceAlertService.deleteAlert(1L, 20L))
                .isInstanceOf(PriceAlertException.class);
    }

    @DisplayName("현재가가 목표가 이상이면 알림을 발송하고 알림을 삭제")
    @Test
    void 현재가가_목표가_이상이면_알림을_발송하고_알림을_삭제() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        PriceAlert alert = alert(1L);
        given(priceAlertRepository.findAll()).willReturn(List.of(alert));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "80000", "1000", "2", "1.26", "88800", "49900"));

        priceAlertService.dispatchAlerts();

        verify(notificationService).createNotification(
                eq(1L), eq(NotificationType.PRICE_ALERT), anyString(), anyString(), anyString());
        verify(priceAlertRepository).delete(alert);
    }

    @DisplayName("현재가가 목표가를 초과하면 알림을 발송하지 않음")
    @Test
    void 현재가가_목표가를_초과하면_알림을_발송하지_않음() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        PriceAlert alert = PriceAlert.builder()
                .userId(1L)
                .stockId(1L)
                .targetPrice(new BigDecimal("80000"))
                .direction(PriceAlertDirection.BELOW)
                .build();
        given(priceAlertRepository.findAll()).willReturn(List.of(alert));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));
        given(kisQuoteClient.fetchQuote("005930")).willReturn(new KisQuoteResponse.Output(
                "85000", "1000", "2", "1.19", "88800", "49900"));

        priceAlertService.dispatchAlerts();

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), anyString());
        verify(priceAlertRepository, never()).delete(any(PriceAlert.class));
    }

    @DisplayName("등록된 알림이 없으면 아무 것도 조회하지 않음")
    @Test
    void 등록된_알림이_없으면_아무_것도_조회하지_않음() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        given(priceAlertRepository.findAll()).willReturn(List.of());

        priceAlertService.dispatchAlerts();

        verify(stockRepository, never()).findAllById(any());
    }

    @DisplayName("현재가 조회에 실패한 알림은 발송하지 않음")
    @Test
    void 현재가_조회에_실패한_알림은_발송하지_않음() {
        priceAlertService = new PriceAlertService(priceAlertRepository, stockRepository, kisQuoteClient, notificationService);
        PriceAlert alert = alert(1L);
        given(priceAlertRepository.findAll()).willReturn(List.of(alert));
        given(stockRepository.findAllById(List.of(1L))).willReturn(List.of(stock()));
        given(kisQuoteClient.fetchQuote("005930")).willThrow(new RuntimeException("KIS 조회 실패"));

        priceAlertService.dispatchAlerts();

        verify(notificationService, never()).createNotification(
                any(), any(), anyString(), anyString(), anyString());
        verify(priceAlertRepository, never()).delete(any(PriceAlert.class));
    }
}