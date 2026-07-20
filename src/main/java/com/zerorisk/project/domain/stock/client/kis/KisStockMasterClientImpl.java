package com.zerorisk.project.domain.stock.client.kis;

import com.zerorisk.project.domain.stock.client.kis.dto.StockMasterRow;
import com.zerorisk.project.domain.stock.entity.Market;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisStockMasterClientImpl implements KisStockMasterClient {

    private static final int PART2_LENGTH = 228;
    private static final int STANDARD_CODE_END = 21;
    private static final int SHORT_CODE_END = 9;
    private static final Charset MASTER_FILE_CHARSET = Charset.forName("MS949");

    private final WebClient stockMasterFileWebClient;
    private final KisStockMasterProperties kisStockMasterProperties;

    @Override
    public List<StockMasterRow> fetchAll() {
        List<StockMasterRow> rows = new ArrayList<>();
        rows.addAll(fetchMarket(kisStockMasterProperties.kospiUrl(), Market.KOSPI));
        rows.addAll(fetchMarket(kisStockMasterProperties.kosdaqUrl(), Market.KOSDAQ));
        return rows;
    }

    private List<StockMasterRow> fetchMarket(String url, Market market) {
        byte[] zipBytes = stockMasterFileWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        if (zipBytes == null || zipBytes.length == 0) {
            log.warn("{} 종목 마스터 파일을 내려받지 못했습니다. (url={})", market, url);
            return List.of();
        }

        return parse(zipBytes, market);
    }

    private List<StockMasterRow> parse(byte[] zipBytes, Market market) {
        List<StockMasterRow> rows = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), MASTER_FILE_CHARSET)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, MASTER_FILE_CHARSET));
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(line, market).ifPresent(rows::add);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(market + " 종목 마스터 파일 파싱에 실패했습니다.", e);
        }
        return rows;
    }

    // package-private: 테스트에서 파싱 로직만 직접 검증하기 위해 접근 범위를 넓혀둔다.
    Optional<StockMasterRow> parseLine(String line, Market market) {
        if (line.length() <= PART2_LENGTH) {
            return Optional.empty();
        }

        String part1 = line.substring(0, line.length() - PART2_LENGTH);
        if (part1.length() < STANDARD_CODE_END) {
            return Optional.empty();
        }

        String rawCode = part1.substring(0, SHORT_CODE_END).trim();
        String standardCode = part1.substring(SHORT_CODE_END, STANDARD_CODE_END).trim();
        String name = part1.substring(STANDARD_CODE_END).trim();

        if (rawCode.isEmpty() || name.isEmpty()) {
            return Optional.empty();
        }

        String code = normalizeCode(rawCode);
        return Optional.of(new StockMasterRow(code, standardCode, name, market));
    }

    private String normalizeCode(String rawCode) {
        String digitsOnly = rawCode.replaceAll("\\D", "");
        if (digitsOnly.length() >= 6) {
            return digitsOnly.substring(digitsOnly.length() - 6);
        }
        return rawCode;
    }
}