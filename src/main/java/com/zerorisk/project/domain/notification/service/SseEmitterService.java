package com.zerorisk.project.domain.notification.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 주의: 기획서 전제대로 단일 EC2 인스턴스 기준 구현.
// 서버를 여러 대로 스케일아웃하면 이 Map은 인스턴스별로 따로 놀기 때문에
// Redis Pub/Sub 등으로 교체 필요.
@Service
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        sendToEmitter(emitter, "connect", "SSE 연결 완료");

        return emitter;
    }

    public void send(Long userId, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            return;
        }

        sendToEmitter(emitter, "notification", data);
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitters.remove(findUserIdByEmitter(emitter));
        }
    }

    private Long findUserIdByEmitter(SseEmitter emitter) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getValue() == emitter)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}