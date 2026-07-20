package com.example.delivery.deploy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeployPlanRequest(
        @NotBlank(message = "部署方案编码不能为空")
        @Size(max = 64, message = "部署方案编码不能超过 64 个字符")
        String planCode,

        @NotBlank(message = "部署方案名称不能为空")
        @Size(max = 128, message = "部署方案名称不能超过 128 个字符")
        String planName,

        @Size(max = 512, message = "部署方案说明不能超过 512 个字符")
        String description
) {
}
