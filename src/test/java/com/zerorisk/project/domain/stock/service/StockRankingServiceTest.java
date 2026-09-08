package com.zerorisk.project.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.zerorisk.project.domain.stock.client.kis.KisRankingClient;
import com.zerorisk.project.domain.stock.client.kis.dto.KisRankingResponse;
import com.zerorisk.project.domain.stock.dto.RankingType;
import com.zerorisk.project.domain.stock.dto.StockRankingResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockRankingServiceTest {

    @Mock
    private KisRankingClient kisRankingClient;

    private StockRankingService stockRankingService;

    @DisplayName("RISE 조회 시 등락률 내림차순 정렬")
    @Test
    void RISE_조회_시_등락률_내림차순_정렬() {
        stockRankingService = new StockRankingService(kisRankingClient);
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "500"),
                new KisRankingResponse.Output("000002", "B", "20000", "600", "2", "3.00", "300"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "900")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.RISE, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000002", "000001", "000003");
    }

    @DisplayName("FALL 조회 시 등락률 오름차순 정렬")
    @Test
    void FALL_조회_시_등락률_오름차순_정렬() {
        stockRankingService = new StockRankingService(kisRankingClient);
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "500"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "900")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.FALL, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000003", "000001");
        assertThat(result.get(0).changeAmount()).isEqualTo(-200L);
    }

    @DisplayName("count로 상위 N건 제한")
    @Test
    void count로_상위_N건_제한() {
        stockRankingService = new StockRankingService(kisRankingClient);
        given(kisRankingClient.fetchVolumeRanking(anyString())).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900"),
                new KisRankingResponse.Output("000002", "B", "20000", "600", "2", "3.00", "500"),
                new KisRankingResponse.Output("000003", "C", "30000", "200", "5", "-2.00", "300")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 2);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000001", "000002");
    }

    @DisplayName("전체/코스닥 두 시장 조회 결과를 합치고 중복 종목은 한 번만 남긴다")
    @Test
    void 두_시장_조회_결과를_합치고_중복은_제거한다() {
        stockRankingService = new StockRankingService(kisRankingClient);
        given(kisRankingClient.fetchVolumeRanking(eq("0000"))).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900")));
        given(kisRankingClient.fetchVolumeRanking(eq("1001"))).willReturn(List.of(
                new KisRankingResponse.Output("000001", "A", "10000", "100", "2", "1.00", "900"),
                new KisRankingResponse.Output("000099", "코스닥전용", "5000", "50", "2", "1.00", "700")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactlyInAnyOrder("000001", "000099");
    }

    @DisplayName("한 시장 조회가 실패해도 다른 시장 결과는 반환한다")
    @Test
    void 한_시장_조회_실패해도_나머지_결과는_반환한다() {
        stockRankingService = new StockRankingService(kisRankingClient);
        given(kisRankingClient.fetchVolumeRanking(eq("0000")))
                .willThrow(new IllegalStateException("KIS 오류"));
        given(kisRankingClient.fetchVolumeRanking(eq("1001"))).willReturn(List.of(
                new KisRankingResponse.Output("000099", "코스닥전용", "5000", "50", "2", "1.00", "700")));

        List<StockRankingResponse> result = stockRankingService.getRankings(RankingType.VOLUME, 10);

        assertThat(result).extracting(StockRankingResponse::code)
                .containsExactly("000099");
    }
}
