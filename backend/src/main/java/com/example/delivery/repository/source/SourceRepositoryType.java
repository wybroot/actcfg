package com.example.delivery.repository.source;

/** 源仓库类型。当前只实现 HARBOR，其余为预留（表结构已支持，同步逻辑暂未分支）。 */
public enum SourceRepositoryType {
    HARBOR,
    NEXUS,
    MAVEN,
    GENERIC
}
