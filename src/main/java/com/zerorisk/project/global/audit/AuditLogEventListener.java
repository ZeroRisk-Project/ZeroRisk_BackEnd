package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final AdminActionLogRepository adminActionLogRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    @Async("auditLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAdminAction(AdminActionEvent event) {
        try {
            adminActionLogRepository.save(
                    AdminActionLog.builder()
                            .adminId(event.adminId())
                            .actionType(event.actionType())
                            .targetType(event.targetType())
                            .targetId(event.targetId())
                            .detail(maskSensitive(event.detail()))
                            .ipAddress(event.ipAddress())
                            .build()
            );
        } catch (Exception e) {
            log.error("[AUDIT_LOG_SAVE_FAILED] admin action log 저장 실패 - adminId: {}, actionType: {}, detail: {}",
                    event.adminId(), event.actionType(), event.detail(), e);
        }
    }

    @Async("auditLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserActivity(UserActivityEvent event) {
        try {
            userActivityLogRepository.save(
                    UserActivityLog.builder()
                            .userId(event.userId())
                            .actionType(event.actionType())
                            .detail(maskSensitive(event.detail()))
                            .ipAddress(event.ipAddress())
                            .build()
            );
        } catch (Exception e) {
            log.error("[AUDIT_LOG_SAVE_FAILED] user activity log 저장 실패 - userId: {}, actionType: {}, detail: {}",
                    event.userId(), event.actionType(), event.detail(), e);
        }
    }

    private String maskSensitive(String detail) {
        if (detail == null) return null;
        return detail.replaceAll("\\d{2,6}-\\d{2,6}-\\d{2,6}", "***-****-****");
    }
}
