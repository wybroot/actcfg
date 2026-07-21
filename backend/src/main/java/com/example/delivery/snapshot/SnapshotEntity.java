package com.example.delivery.snapshot;

import java.time.LocalDateTime;

/**
 * 客户配置快照主记录：客户环境绑定部署方案版本时深拷贝生成的独立副本。
 */
public record SnapshotEntity(
        Long id,
        Long customerId,
        Long environmentId,
        Long sourcePlanVersionId,
        String planName,
        String versionLabel,
        String status,
        LocalDateTime createdAt
) {}
