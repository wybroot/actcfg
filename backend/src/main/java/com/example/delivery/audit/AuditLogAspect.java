package com.example.delivery.audit;

import com.example.delivery.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计切面：拦截 @AuditLog 标注的方法，成功/失败均记录操作日志（操作人、IP、结果）。
 */
@Aspect
@Component
@Order(100) // 低于 Spring Security @PreAuthorize 拦截器(400)，使切面包住权限校验，从而记录被拒绝的写操作
public class AuditLogAspect {

    private final AuditService auditService;

    public AuditLogAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String operator = currentOperator();
        String ip = currentIp();
        String method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        try {
            Object result = joinPoint.proceed();
            auditService.recordOperation(operator, auditLog.module(), auditLog.action(),
                    "SUCCESS", ip, method);
            return result;
        } catch (Throwable ex) {
            auditService.recordOperation(operator, auditLog.module(), auditLog.action(),
                    "FAILED", ip, method + ": " + ex.getMessage());
            throw ex;
        }
    }

    private String currentOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser user) {
            return user.username();
        }
        return "anonymous";
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // 非 web 上下文（如测试）忽略
        }
        return null;
    }
}
