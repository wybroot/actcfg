package com.example.delivery.audit;

import java.time.LocalDateTime;

public record DownloadLogEntity(Long id, String downloaderName, String targetType, String targetName, String ipAddress, LocalDateTime createdAt) {
}
