package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityLogger {

    private final ApplicationEventPublisher eventPublisher;

    public void log(Long userId, String actionType, String detail) {
        eventPublisher.publishEvent(new UserActivityEvent(
                userId, actionType, detail, ClientIpExtractor.extract()
        ));
    }
}
