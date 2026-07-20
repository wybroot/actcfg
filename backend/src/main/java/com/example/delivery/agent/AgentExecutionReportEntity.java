package com.example.delivery.agent;

import java.time.LocalDateTime;

public record AgentExecutionReportEntity(
        Long id,
        String reportCode,
        Long taskId,
        Long packageBuildId,
        Long customerId,
        Long environmentId,
        String executionHost,
        AgentTaskStatus executionStatus,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String failedStep,
        String failureReason,
        String healthCheckResult,
        String reportContent,
        LocalDateTime importedAt
) {
}
