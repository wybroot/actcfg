package com.example.delivery.customer;

public record CustomerEnvironmentEntity(
        Long id,
        Long customerId,
        String environmentName,
        EnvironmentType environmentType,
        Long deployPlanVersionId,
        String status
) {
}
