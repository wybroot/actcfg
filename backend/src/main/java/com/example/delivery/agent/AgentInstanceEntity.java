package com.example.delivery.agent;

import java.time.LocalDateTime;

/**
 * 在线 Agent 实例：客户现场注册的 agent，通过心跳保活，拉取并执行任务。
 */
public record AgentInstanceEntity(
        Long id,
        String agentCode,
        String hostname,
        String ipAddress,
        Long customerId,
        Long environmentId,
        String instanceStatus,   // ONLINE / OFFLINE
        LocalDateTime lastHeartbeatAt,
        LocalDateTime registeredAt
) {}
