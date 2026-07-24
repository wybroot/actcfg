package com.example.delivery.agent.executor;

/**
 * 离线部署执行步骤类型，对应 agent 脚本中的实际动作。
 */
public enum DeployStepType {
    CHECK_ENV,        // 环境检测：OS/CPU/磁盘/Docker/端口
    COMPAT_CHECK,     // 包与目标环境兼容性校验
    LOAD_IMAGE,       // 加载镜像（docker load）
    RENDER_CONFIG,    // 渲染配置模板
    DB_INIT,          // 数据库初始化（执行 SQL）
    DEPLOY_ARTIFACT,  // 部署制品（jar/脚本等）
    HEALTH_CHECK      // 健康检查
}
