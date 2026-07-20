package com.example.delivery.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportAgentStatusRequest(
        @NotNull(message = "任务状态不能为空")
        AgentTaskStatus taskStatus,

        @Size(max = 1024, message = "结果摘要不能超过 1024 个字符")
        String resultSummary,

        @NotBlank(message = "步骤编码不能为空")
        @Size(max = 64, message = "步骤编码不能超过 64 个字符")
        String stepCode,

        @NotBlank(message = "步骤名称不能为空")
        @Size(max = 128, message = "步骤名称不能超过 128 个字符")
        String stepName,

        @Size(max = 16, message = "日志级别不能超过 16 个字符")
        String logLevel,

        @Size(max = 2048, message = "日志内容不能超过 2048 个字符")
        String logContent
) {
}
