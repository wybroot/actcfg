package com.example.delivery.customer;

import jakarta.validation.constraints.NotNull;

public record BindDeployPlanRequest(
        @NotNull(message = "部署方案版本不能为空")
        Long deployPlanVersionId
) {
}
