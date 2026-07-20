package com.example.delivery.packagebuild;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePackageBuildRequest(
        @NotNull(message = "客户不能为空")
        Long customerId,

        @NotNull(message = "客户环境不能为空")
        Long environmentId,

        @NotNull(message = "部署方案版本不能为空")
        Long deployPlanVersionId,

        @NotBlank(message = "部署包版本不能为空")
        @Size(max = 64, message = "部署包版本不能超过 64 个字符")
        String packageVersion,

        @Size(max = 512, message = "备注不能超过 512 个字符")
        String remark
) {
}
