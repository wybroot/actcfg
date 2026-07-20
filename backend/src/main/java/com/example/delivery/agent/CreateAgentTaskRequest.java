package com.example.delivery.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAgentTaskRequest(
        @NotNull(message = "部署包不能为空")
        Long packageBuildId,

        @NotBlank(message = "任务类型不能为空")
        @Size(max = 64, message = "任务类型不能超过 64 个字符")
        String taskType
) {
}
