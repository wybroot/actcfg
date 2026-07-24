package com.example.delivery.repository.source;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新源仓库请求。password 留空表示不修改原密码；非空则加密覆盖。
 */
public record UpdateSourceRepositoryRequest(
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
