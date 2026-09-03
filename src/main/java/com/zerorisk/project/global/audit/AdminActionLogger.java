package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminActionLogger {

    private final ApplicationEventPublisher eventPublisher;

    public void log(Long adminId, String actionType, String targetType, Long targetId, String detail) {
        AuditLogTracker.markLogged();
        eventPublisher.publishEvent(new AdminActionEvent(
                adminId, actionType, targetType, targetId, detail, ClientIpExtractor.extract()
        ));
    }
}
