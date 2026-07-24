package com.example.delivery.customer;

public record EnvVariableEntity(
        Long id,
        Long environmentId,
        String variableKey,
        String variableValue,   // 实际值；sensitive=true 时前端展示 maskedValue
        String maskedValue,
        boolean sensitive
) {}
