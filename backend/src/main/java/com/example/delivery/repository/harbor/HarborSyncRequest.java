package com.example.delivery.repository.harbor;

import jakarta.validation.constraints.NotBlank;

/**
 * Harbor 镜像同步请求：指定目标资源、镜像坐标，平台从 Harbor 拉取 digest 后登记资源版本。
 */
public record HarborSyncRequest(
        /** 源仓库 ID；指定时用该源仓库地址/凭证，为空则回退全局 app.harbor 配置 */
        Long sourceRepositoryId,

        @NotBlank(message = "Harbor 项目名不能为空")
        String project,

        @NotBlank(message = "镜像仓库名不能为空")
        String repository,

        @NotBlank(message = "镜像标签不能为空")
        String tag,

        /** 版本号；不填时使用 tag */
        String version,

        String releaseNote
) {}
