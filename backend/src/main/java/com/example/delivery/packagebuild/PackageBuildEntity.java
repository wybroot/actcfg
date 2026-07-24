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
        LocalDateTime createdAt,
        PackageLifecycleState lifecycleState,
        long downloadCount,
        LocalDateTime lastDownloadedAt,
        LocalDateTime retentionUntil
) {
    /** 复制并覆盖生命周期字段，用于状态流转/下载计数更新。 */
    public PackageBuildEntity withLifecycle(PackageLifecycleState state, long downloadCount,
                                            LocalDateTime lastDownloadedAt, LocalDateTime retentionUntil) {
        return new PackageBuildEntity(id, packageCode, customerId, environmentId, deployPlanVersionId,
                packageVersion, buildStatus, immutable, filePath, checksum, createdAt,
                state, downloadCount, lastDownloadedAt, retentionUntil);
    }
}
