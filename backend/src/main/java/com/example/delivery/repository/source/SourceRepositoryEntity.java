package com.example.delivery.repository.source;

import java.time.LocalDateTime;

/**
 * 源仓库实体。password 字段语义随场景不同：
 * - 存储层（DB/内存）中保存密文（enc: 前缀）
 * - 对外 list/get 返回时脱敏为掩码
 * - 内部同步取凭证时解密为明文
 */
public record SourceRepositoryEntity(
        Long id,
        String repoCode,
        String repoName,
        SourceRepositoryType repoType,
        String baseUrl,
        String username,
        String password,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
