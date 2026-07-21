package com.example.delivery.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAgentRequest(
        @NotBlank(message = "agent 编码不能为空")
        @Size(max = 64, message = "agent 编码不能超过 64 个字符")
        String agentCode,

        @Size(max = 128, message = "主机名不能超过 128 个字符")
        String hostname,

        Long customerId,

        Long environmentId
) {}
