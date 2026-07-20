package com.example.delivery.agent;

import java.time.LocalDateTime;

public record AgentRetryRecordView(
        Long taskId,
        String taskCode,
        Long packageBuildId,
        String failedStep,
        String failureReason,
        int retryCount,
        LocalDateTime lastRetryAt,
        AgentTaskStatus finalStatus
) {
}
