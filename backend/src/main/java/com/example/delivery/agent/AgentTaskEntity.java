package com.example.delivery.agent;

import java.time.LocalDateTime;

public record AgentTaskEntity(
        Long id,
        String taskCode,
        Long packageBuildId,
        String taskType,
        AgentTaskStatus taskStatus,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String resultSummary
) {
}
