package com.example.delivery.agent;

import java.time.LocalDateTime;

public record AgentRetryRecordEntity(
        Long id,
        Long taskId,
        int retryNo,
        String failedStep,
        String failureReason,
        LocalDateTime triggeredAt,
        LocalDateTime finishedAt,
        AgentTaskStatus resultStatus
) {
}
