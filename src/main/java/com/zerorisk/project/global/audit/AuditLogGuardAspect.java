package com.zerorisk.project.global.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// 감사 로그 누락 감지 안전망. 실제 로그 내용은 지금처럼 개발자가 AdminActionLogger.log()를
// 명시적으로 호출해서 남기고, 이 애스펙트는 "관리자 쓰기 API가 실행됐는데 이번 요청에서
// 로그 호출이 한 번도 없었다"만 감지해서 서버 경고 로그로 남긴다 (자동 로깅 아님).
@Slf4j
@Aspect
@Component
public class AuditLogGuardAspect {

    @Around("execution(* com.zerorisk.project.domain.*.controller.Admin*Controller.*(..)) "
            + "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.PutMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) "
            + "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object guard(ProceedingJoinPoint joinPoint) throws Throwable {
        AuditLogTracker.reset();
        try {
            Object result = joinPoint.proceed();
            if (!AuditLogTracker.wasLogged()) {
                log.warn("[AUDIT_LOG_MISSING] {} 실행됐지만 감사 로그가 기록되지 않음. "
                                + "AdminActionLogger.log() 호출을 빠뜨렸을 가능성이 있습니다.",
                        joinPoint.getSignature().toShortString());
            }
            return result;
        } finally {
            AuditLogTracker.clear();
        }
    }
}
