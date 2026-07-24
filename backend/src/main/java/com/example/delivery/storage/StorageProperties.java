package com.example.delivery.storage;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String type,
        String basePath,
        Integer packageRetentionDays,
        Minio minio
) {
    /** 部署包默认保留天数（未配置时 90 天）。 */
    public int retentionDays() {
        return packageRetentionDays != null && packageRetentionDays > 0 ? packageRetentionDays : 90;
    }

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
