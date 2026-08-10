package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminActionLogger {

    private final AdminActionLogRepository adminActionLogRepository;

    public void log(Long adminId, String actionType, String targetType, Long targetId, String detail) {
        adminActionLogRepository.save(
                AdminActionLog.builder()
                        .adminId(adminId)
                        .actionType(actionType)
                        .targetType(targetType)
                        .targetId(targetId)
                        .detail(detail)
                        .build());
    }
}
