package com.example.delivery.storage;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String type,
        String basePath,
        Minio minio
) {
    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            Buckets buckets
    ) {
    }

    public record Buckets(
            String resources,
            String packages,
            String agents,
            String reports
    ) {
        public Map<String, String> asMap() {
            return Map.of(
                    "resources", resources,
                    "packages", packages,
                    "agents", agents,
                    "reports", reports
            );
        }
    }
}
