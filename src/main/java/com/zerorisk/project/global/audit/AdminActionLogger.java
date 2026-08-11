package com.zerorisk.project.global.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
                        .ipAddress(extractClientIp())
                        .build());
    }

    private String extractClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
