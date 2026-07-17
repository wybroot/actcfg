package com.example.delivery.repository;

import java.time.LocalDateTime;

public record ResourceVersionEntity(
        Long id,
        Long resourceId,
        String version,
        String externalUrl,
        String imageRepository,
        String imageTag,
        String checksum,
        String releaseNote,
        String status,
        LocalDateTime createdAt
) {
}
