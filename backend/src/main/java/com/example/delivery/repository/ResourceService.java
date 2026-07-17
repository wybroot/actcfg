package com.example.delivery.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

    public List<ResourceEntity> listResources() {
        return List.of(new ResourceEntity(
                1L,
                "RES-APP-001",
                "示例应用服务",
                ResourceType.JAR,
                ResourceSourceType.UPLOAD,
                "MVP 示例资源",
                "ENABLED",
                LocalDateTime.now()
        ));
    }

    public List<ResourceVersionEntity> listVersions(Long resourceId) {
        return List.of(new ResourceVersionEntity(
                1L,
                resourceId,
                "1.0.0",
                null,
                null,
                null,
                "sha256-placeholder",
                "初始版本",
                "ENABLED",
                LocalDateTime.now()
        ));
    }
}
