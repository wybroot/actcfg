package com.example.delivery.agent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ImportAgentReportRequest(
        @NotNull(message = "任务不能为空")
        Long taskId,

        @Size(max = 128, message = "执行主机不能超过 128 个字符")
        String executionHost,

        @Size(max = 64, message = "失败步骤不能超过 64 个字符")
        String failedStep,

        @Size(max = 1024, message = "失败原因不能超过 1024 个字符")
        String failureReason,

        @Size(max = 512, message = "健康检查结果不能超过 512 个字符")
        String healthCheckResult,

        @Size(max = 8192, message = "报告内容不能超过 8192 个字符")
        String reportContent
) {
}
