package com.example.delivery.repository;

import java.time.LocalDateTime;

public record ResourceEntity(
        Long id,
        String resourceCode,
        String resourceName,
        ResourceType resourceType,
        ResourceSourceType sourceType,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
