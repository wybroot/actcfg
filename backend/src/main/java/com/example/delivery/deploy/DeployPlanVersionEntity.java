package com.example.delivery.deploy;

import java.time.LocalDateTime;

public record DeployPlanVersionEntity(
        Long id,
        Long planId,
        String version,
        DeployPlanVersionStatus status,
        boolean editable,
        LocalDateTime createdAt
) {
    public boolean canEdit() {
        return status == DeployPlanVersionStatus.DRAFT;
    }
}
