package com.example.delivery.packagebuild;

/**
 * 部署包生命周期状态（与构建结果 {@link PackageBuildStatus} 正交）。
 * <p>流转：ACTIVE → ARCHIVED → DEPRECATED → PURGED（清理后 zip 已删除，DB 记录保留供审计）。
 */
public enum PackageLifecycleState {
    /** 活跃：可下载。 */
    ACTIVE,
    /** 归档：标记为旧版，仍可下载。 */
    ARCHIVED,
    /** 废弃：禁止下载，等待清理。 */
    DEPRECATED,
    /** 已清理：物理文件已删除，仅保留元数据。 */
    PURGED;

    /** 是否允许下载（仅活跃/归档态）。 */
    public boolean downloadable() {
        return this == ACTIVE || this == ARCHIVED;
    }
}
