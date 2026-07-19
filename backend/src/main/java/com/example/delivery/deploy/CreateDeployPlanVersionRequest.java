package com.example.delivery.deploy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeployPlanVersionRequest(
        @NotBlank(message = "版本号不能为空")
        @Size(max = 64, message = "版本号不能超过 64 个字符")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "版本号只能包含字母、数字、点、下划线和短横线")
        String version
) {
}
