package com.example.delivery.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
        @NotBlank(message = "资源名称不能为空")
        @Size(max = 128, message = "资源名称不能超过 128 个字符")
        String resourceName,

        @NotNull(message = "资源类型不能为空")
        ResourceType resourceType,

        @NotNull(message = "资源来源不能为空")
        ResourceSourceType sourceType,

        @Size(max = 512, message = "资源说明不能超过 512 个字符")
        String description,

        @NotBlank(message = "资源状态不能为空")
        @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "资源状态只能是 ENABLED 或 DISABLED")
        String status
) {
}
