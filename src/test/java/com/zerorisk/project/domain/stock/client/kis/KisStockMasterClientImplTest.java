package com.zerorisk.project.domain.stock.client.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import com.zerorisk.project.domain.stock.entity.Market;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KisStockMasterClientImplTest {

    private final KisStockMasterClientImpl client =
            new KisStockMasterClientImpl(null, new KisStockMasterProperties(null, null));

    @DisplayName("정상 라인을 파싱 시 코드 표준코드 이름을 추출")
    @Test
    void 정상_라인을_파싱_시_코드_표준코드_이름을_추출() {
        String shortCode = pad("A005930", 9);
        String standardCode = pad("KR7005930003", 12);
        String name = "삼성전자";
        String part2 = "9".repeat(228);
        String line = shortCode + standardCode + name + part2;

        Optional<StockMasterRow> result = client.parseLine(line, Market.KOSPI);

        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("005930");
        assertThat(result.get().standardCode()).isEqualTo("KR7005930003");
        assertThat(result.get().name()).isEqualTo(name);
        assertThat(result.get().market()).isEqualTo(Market.KOSPI);
    }

    @DisplayName("뒷부분 228자보다 짧은 라인은 무시")
    @Test
    void 뒷부분_228자보다_짧은_라인은_무시() {
        String tooShort = "A005930".repeat(10);

        Optional<StockMasterRow> result = client.parseLine(tooShort, Market.KOSPI);

        assertThat(result).isEmpty();
    }

    @DisplayName("이름이 비어있는 라인은 무시")
    @Test
    void 이름이_비어있는_라인은_무시() {
        String shortCode = pad("A005930", 9);
        String standardCode = pad("KR7005930003", 12);
        String part2 = "9".repeat(228);
        String line = shortCode + standardCode + part2;

        Optional<StockMasterRow> result = client.parseLine(line, Market.KOSPI);

        assertThat(result).isEmpty();
    }

    private String pad(String value, int length) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }
}