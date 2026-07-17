package com.example.delivery.deploy;

import java.time.LocalDateTime;

public record DeployPlanEntity(
        Long id,
        String planCode,
        String planName,
        Long currentVersionId,
        String status,
        LocalDateTime createdAt
) {
}
