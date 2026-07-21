package com.example.delivery.agent.executor;

import java.util.List;

/**
 * 离线部署执行计划：由部署包的组件推导出的有序步骤序列，供 agent 脚本执行。
 */
public record ExecutionPlan(
        String packageCode,
        List<DeployStep> steps
) {}
