package com.example.delivery.repository.source;

import jakarta.validation.constraints.NotBlank;

/**
 * 新建源仓库请求。password 为明文，服务层加密后入库。
 */
public record CreateSourceRepositoryRequest(
        @NotBlank(message = "源仓库编码不能为空")
        String repoCode,

        @NotBlank(message = "源仓库名称不能为空")
        String repoName,

        SourceRepositoryType repoType,

        @NotBlank(message = "仓库地址不能为空")
        String baseUrl,

        String username,
        String password,
        String description,
        String status
) {
}
