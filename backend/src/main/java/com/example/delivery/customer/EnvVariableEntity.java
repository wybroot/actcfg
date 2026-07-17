package com.example.delivery.customer;

public record EnvVariableEntity(Long id, Long environmentId, String variableKey, String maskedValue, boolean sensitive) {
}
