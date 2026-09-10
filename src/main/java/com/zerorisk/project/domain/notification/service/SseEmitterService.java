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

    // 반환값은 "실제로 연결된 클라이언트에 전송했는지"를 뜻한다 - 단순히 유저가 오프라인이라
    // emitter가 없는 경우는 실패가 아니라 정상적인 상황이라 false만 돌려주고 예외는 던지지 않는다
    // (호출부의 @Retryable이 이 경우까지 재시도/DLQ 격리하지 않도록).
    public boolean send(Long userId, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter == null) {
            return false;
        }

        return sendToEmitter(emitter, "notification", data);
    }

    private boolean sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException | IllegalStateException e) {
            // IllegalStateException은 이미 완료(completed)된 emitter에 보내려 할 때 발생한다.
            // 이것도 못 잡으면 죽은 emitter가 맵에 계속 남아 다음 알림마다 똑같이 재시도만 낭비한다.
            emitters.remove(findUserIdByEmitter(emitter));
            return false;
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