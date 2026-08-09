package com.zerorisk.project.domain.stock.client.kis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.domain.stock.client.kis.dto.KisApprovalResponse;
import com.zerorisk.project.domain.stock.ws.StockPriceWebSocketHandler;
import com.zerorisk.project.domain.stock.ws.dto.StockPriceMessage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimeClientImpl implements KisRealtimeClient {

    private static final String TR_ID = "H0STCNT0";
    private static final Set<String> NEGATIVE_SIGNS = Set.of("4", "5");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;
    private final KisRealtimeProperties kisRealtimeProperties;
    private final StockPriceWebSocketHandler stockPriceWebSocketHandler;

    private volatile String approvalKey;
    private volatile WebSocketSession kisSession;

    @Override
    public synchronized void subscribe(String code) {
        ensureConnected();
        sendFrame(code, "1");
    }

    @Override
    public synchronized void unsubscribe(String code) {
        if (kisSession == null || !kisSession.isOpen()) {
            return;
        }
        sendFrame(code, "2");
    }

    private void ensureConnected() {
        if (kisSession != null && kisSession.isOpen()) {
            return;
        }

        approvalKey = issueApprovalKey();
        try {
            kisSession = new StandardWebSocketClient()
                    .doHandshake(new KisRealtimeFrameHandler(), new WebSocketHttpHeaders(), URI.create(kisRealtimeProperties.wsUrl()))
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("KIS 실시간 시세 WebSocket 연결에 실패했습니다.", e);
        }
    }

    private void sendFrame(String code, String trType) {
        try {
            Map<String, Object> frame = Map.of(
                    "header", Map.of(
                            "approval_key", approvalKey,
                            "custtype", "P",
                            "tr_type", trType,
                            "content-type", "utf-8"),
                    "body", Map.of(
                            "input", Map.of(
                                    "tr_id", TR_ID,
                                    "tr_key", code)));

            kisSession.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(frame)));
        } catch (IOException e) {
            throw new IllegalStateException("KIS 실시간 시세 구독 요청 전송에 실패했습니다. code=" + code, e);
        }
    }

    private String issueApprovalKey() {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisProperties.appKey(),
                "secretkey", kisProperties.appSecret());

        KisApprovalResponse response = kisWebClient.post()
                .uri("/oauth2/Approval")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(KisApprovalResponse.class)
                .block();

        if (response == null || response.approvalKey() == null) {
            throw new IllegalStateException("KIS 실시간 시세 접속키 발급에 실패했습니다.");
        }

        return response.approvalKey();
    }

    private class KisRealtimeFrameHandler extends TextWebSocketHandler {

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            parseAndBroadcast(message.getPayload());
        }
    }

    private void parseAndBroadcast(String payload) {
        if (!payload.startsWith("0|") && !payload.startsWith("1|")) {
            return;
        }

        String[] pipeParts = payload.split("\\|", 4);
        if (pipeParts.length < 4 || !TR_ID.equals(pipeParts[1])) {
            return;
        }

        String[] fields = pipeParts[3].split("\\^");
        if (fields.length < 6) {
            return;
        }

        try {
            String code = fields[0];
            long currentPrice = Long.parseLong(fields[2]);
            String changeSign = fields[3];
            long changeAmount = Long.parseLong(fields[4]);
            if (NEGATIVE_SIGNS.contains(changeSign)) {
                changeAmount = -changeAmount;
            }
            BigDecimal changeRate = new BigDecimal(fields[5]);

            stockPriceWebSocketHandler.broadcast(
                    code, new StockPriceMessage(code, currentPrice, changeAmount, changeRate));
        } catch (NumberFormatException e) {
            log.warn("KIS 실시간 시세 파싱 실패 - payload: {}", payload, e);
        }
    }
}