package com.example.delivery.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要记录审计操作日志的写操作方法。由 AuditLogAspect 拦截。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /** 模块，如 CUSTOMER / RESOURCE / DEPLOY_PLAN / PACKAGE / ENV_VARIABLE / AGENT。 */
    String module();

    /** 动作，如 CREATE / UPDATE / DELETE / PUBLISH / BUILD / BIND。 */
    String action();
}
