package com.example.delivery.agent;

public record AgentExecutionLogEntity(
        Long id,
        Long taskId,
        String stepCode,
        String stepName,
        AgentTaskStatus stepStatus,
        String logLevel,
        String logContent,
        int retryCount
) {
}
