package com.zerorisk.project.domain.stock.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerorisk.project.domain.stock.client.kis.KisRealtimeClient;
import com.zerorisk.project.domain.stock.ws.dto.StockPriceMessage;
import com.zerorisk.project.domain.stock.ws.dto.StockSubscribeMessage;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class StockPriceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> codeSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionCodes = new ConcurrentHashMap<>();
    private final KisRealtimeClient kisRealtimeClient;

    public StockPriceWebSocketHandler(@Lazy KisRealtimeClient kisRealtimeClient) {
        this.kisRealtimeClient = kisRealtimeClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionCodes.put(session.getId(), new CopyOnWriteArraySet<>());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        StockSubscribeMessage subscribeMessage = objectMapper.readValue(message.getPayload(), StockSubscribeMessage.class);

        if (subscribeMessage.action() == StockSubscribeMessage.SubscribeAction.SUBSCRIBE) {
            subscribe(session, subscribeMessage.code());
        } else {
            unsubscribe(session, subscribeMessage.code());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Set<String> codes = sessionCodes.remove(session.getId());
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            removeSubscriber(session, code);
        }
    }

    public void broadcast(String code, StockPriceMessage priceMessage) {
        Set<WebSocketSession> sessions = codeSubscribers.get(code);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(priceMessage));
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("실시간 시세 브로드캐스트 실패 - code: {}", code, e);
        }
    }

    private void subscribe(WebSocketSession session, String code) {
        Set<WebSocketSession> subscribers = codeSubscribers.computeIfAbsent(code, key -> new CopyOnWriteArraySet<>());
        boolean firstSubscriber = subscribers.isEmpty();
        subscribers.add(session);
        sessionCodes.get(session.getId()).add(code);

        if (firstSubscriber) {
            kisRealtimeClient.subscribe(code);
        }
    }

    private void unsubscribe(WebSocketSession session, String code) {
        sessionCodes.get(session.getId()).remove(code);
        removeSubscriber(session, code);
    }

    private void removeSubscriber(WebSocketSession session, String code) {
        Set<WebSocketSession> subscribers = codeSubscribers.get(code);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(session);
        if (subscribers.isEmpty()) {
            codeSubscribers.remove(code);
            kisRealtimeClient.unsubscribe(code);
        }
    }
}