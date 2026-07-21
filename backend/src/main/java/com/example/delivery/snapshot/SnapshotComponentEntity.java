package com.example.delivery.snapshot;

/**
 * 快照组件副本，config_template 可独立于源方案编辑。
 */
public record SnapshotComponentEntity(
        Long id,
        Long snapshotId,
        String componentName,
        String componentType,
        Long resourceVersionId,
        int deployOrder,
        String configTemplate,
        String healthCheck
) {}
