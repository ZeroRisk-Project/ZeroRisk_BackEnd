package com.zerorisk.project.domain.stock.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zerorisk.project.domain.stock.client.kis.KisRealtimeClient;
import com.zerorisk.project.domain.stock.ws.dto.StockPriceMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class StockPriceWebSocketHandlerTest {

    @Mock
    private KisRealtimeClient kisRealtimeClient;

    @Mock
    private WebSocketSession session;

    private StockPriceWebSocketHandler handler;

    @DisplayName("첫 구독자가 구독하면 KIS 클라이언트에도 구독 요청")
    @Test
    void 첫_구독자가_구독하면_KIS_클라이언트에도_구독_요청() throws Exception {
        handler = new StockPriceWebSocketHandler(kisRealtimeClient);
        given(session.getId()).willReturn("session-1");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage(
                "{\"action\":\"SUBSCRIBE\",\"code\":\"005930\"}"));

        verify(kisRealtimeClient, times(1)).subscribe("005930");
    }

    @DisplayName("이미 구독 중인 종목에 두 번째 세션이 붙어도 KIS 구독은 한 번만 요청")
    @Test
    void 이미_구독_중인_종목에_두_번째_세션이_붙어도_KIS_구독은_한_번만_요청() throws Exception {
        handler = new StockPriceWebSocketHandler(kisRealtimeClient);
        WebSocketSession secondSession = org.mockito.Mockito.mock(WebSocketSession.class);
        given(session.getId()).willReturn("session-1");
        given(secondSession.getId()).willReturn("session-2");

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(secondSession);

        handler.handleTextMessage(session, new TextMessage("{\"action\":\"SUBSCRIBE\",\"code\":\"005930\"}"));
        handler.handleTextMessage(secondSession, new TextMessage("{\"action\":\"SUBSCRIBE\",\"code\":\"005930\"}"));

        verify(kisRealtimeClient, times(1)).subscribe("005930");
    }

    @DisplayName("마지막 세션이 연결 종료되면 KIS 클라이언트 구독 해지")
    @Test
    void 마지막_세션이_연결_종료되면_KIS_클라이언트_구독_해지() throws Exception {
        handler = new StockPriceWebSocketHandler(kisRealtimeClient);
        given(session.getId()).willReturn("session-1");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"action\":\"SUBSCRIBE\",\"code\":\"005930\"}"));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(kisRealtimeClient, times(1)).unsubscribe("005930");
    }

    @DisplayName("구독하지 않은 세션에는 브로드캐스트하지 않음")
    @Test
    void 구독하지_않은_세션에는_브로드캐스트하지_않음() throws Exception {
        handler = new StockPriceWebSocketHandler(kisRealtimeClient);

        handler.broadcast("005930", new StockPriceMessage("005930", 70000L, 1000L, java.math.BigDecimal.ONE));

        verify(session, never()).sendMessage(any());
    }

    @DisplayName("구독 중인 세션에는 시세 메시지를 전송")
    @Test
    void 구독_중인_세션에는_시세_메시지를_전송() throws Exception {
        handler = new StockPriceWebSocketHandler(kisRealtimeClient);
        given(session.getId()).willReturn("session-1");
        given(session.isOpen()).willReturn(true);
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"action\":\"SUBSCRIBE\",\"code\":\"005930\"}"));

        handler.broadcast("005930", new StockPriceMessage("005930", 70000L, 1000L, java.math.BigDecimal.ONE));

        verify(session, times(1)).sendMessage(any());
    }
}