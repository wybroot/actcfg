package com.example.delivery.packagebuild;

import java.time.LocalDateTime;

public record PackageBuildEntity(
        Long id,
        String packageCode,
        Long customerId,
        Long environmentId,
        Long deployPlanVersionId,
        String packageVersion,
        PackageBuildStatus buildStatus,
        boolean immutable,
        String filePath,
        String checksum,
        LocalDateTime createdAt
) {
}
