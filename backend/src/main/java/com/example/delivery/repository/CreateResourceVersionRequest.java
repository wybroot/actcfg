package com.example.delivery.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateResourceVersionRequest(
        @NotBlank(message = "版本号不能为空")
        @Size(max = 64, message = "版本号不能超过 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "版本号只能包含字母、数字、点、下划线和短横线")
        String version,

        @Size(max = 512, message = "外部地址不能超过 512 个字符")
        String externalUrl,

        @Size(max = 256, message = "镜像仓库不能超过 256 个字符")
        String imageRepository,

        @Size(max = 128, message = "镜像标签不能超过 128 个字符")
        String imageTag,

        @Size(max = 128, message = "checksum 不能超过 128 个字符")
        String checksum,

        @Size(max = 1024, message = "发布说明不能超过 1024 个字符")
        String releaseNote,

        @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "版本状态只能是 ENABLED 或 DISABLED")
        String status
) {
}
