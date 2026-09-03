package com.zerorisk.project.global.audit;

// 이번 요청 스레드에서 AdminActionLogger.log()가 호출됐는지 추적하는 내부 협력 장치.
// AdminActionLogger와 AuditLogGuardAspect 둘만 접근하면 되므로 패키지 프라이빗으로 노출 범위를 최소화한다.
public final class AuditLogTracker {

    private static final ThreadLocal<Boolean> LOGGED = ThreadLocal.withInitial(() -> false);

    private AuditLogTracker() {
    }

    static void markLogged() {
        LOGGED.set(true);
    }

    static boolean wasLogged() {
        return LOGGED.get();
    }

    static void reset() {
        LOGGED.set(false);
    }

    static void clear() {
        LOGGED.remove();
    }
}
