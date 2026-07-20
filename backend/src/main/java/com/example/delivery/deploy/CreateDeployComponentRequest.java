package com.example.delivery.deploy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDeployComponentRequest(
        @NotBlank(message = "组件名称不能为空")
        @Size(max = 128, message = "组件名称不能超过 128 个字符")
        String componentName,

        @NotBlank(message = "组件类型不能为空")
        @Size(max = 32, message = "组件类型不能超过 32 个字符")
        String componentType,

        @NotNull(message = "资源版本不能为空")
        Long resourceVersionId,

        @NotNull(message = "部署顺序不能为空")
        Integer deployOrder,

        @Size(max = 4096, message = "配置模板不能超过 4096 个字符")
        String configTemplate,

        @Size(max = 1024, message = "健康检查规则不能超过 1024 个字符")
        String healthCheck
) {
}
