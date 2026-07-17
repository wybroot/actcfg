package com.example.delivery.deploy;

public record DeployComponentEntity(
        Long id,
        Long planVersionId,
        String componentName,
        String componentType,
        Long resourceVersionId,
        int deployOrder,
        String configTemplate,
        String healthCheck
) {
}
