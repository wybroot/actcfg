package com.example.delivery.repository;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResourceService {
    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";

    private final AtomicLong resourceIdSequence = new AtomicLong(1);
    private final AtomicLong versionIdSequence = new AtomicLong(1);
    private final Map<Long, ResourceEntity> resources = new ConcurrentHashMap<>();
    private final Map<Long, ResourceVersionEntity> versions = new ConcurrentHashMap<>();

    public ResourceService() {
        LocalDateTime now = LocalDateTime.now();
        ResourceEntity resource = new ResourceEntity(
                resourceIdSequence.getAndIncrement(),
                "RES-APP-001",
                "示例应用服务",
                ResourceType.JAR,
                ResourceSourceType.UPLOAD,
                "MVP 示例资源",
                ENABLED,
                now,
                now,
                false
        );
        resources.put(resource.id(), resource);
        ResourceVersionEntity version = new ResourceVersionEntity(
                versionIdSequence.getAndIncrement(),
                resource.id(),
                "1.0.0",
                "internal://repo/example-app-1.0.0.jar",
                null,
                null,
                "sha256-placeholder",
                "初始版本",
                ENABLED,
                now
        );
        versions.put(version.id(), version);
    }

    public List<ResourceEntity> listResources() {
        return resources.values().stream()
                .filter(resource -> !resource.deleted())
                .sorted(Comparator.comparing(ResourceEntity::createdAt).reversed())
                .toList();
    }

    public ResourceEntity getResource(Long id) {
        ResourceEntity resource = resources.get(id);
        if (resource == null || resource.deleted()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    public ResourceEntity createResource(CreateResourceRequest request) {
        ensureResourceCodeUnique(request.resourceCode());
        LocalDateTime now = LocalDateTime.now();
        ResourceEntity resource = new ResourceEntity(
                resourceIdSequence.getAndIncrement(),
                request.resourceCode(),
                request.resourceName(),
                request.resourceType(),
                request.sourceType(),
                request.description(),
                defaultStatus(request.status()),
                now,
                now,
                false
        );
        resources.put(resource.id(), resource);
        return resource;
    }

    public ResourceEntity updateResource(Long id, UpdateResourceRequest request) {
        ResourceEntity current = getResource(id);
        ResourceEntity updated = new ResourceEntity(
                current.id(),
                current.resourceCode(),
                request.resourceName(),
                request.resourceType(),
                request.sourceType(),
                request.description(),
                request.status(),
                current.createdAt(),
                LocalDateTime.now(),
                false
        );
        resources.put(id, updated);
        return updated;
    }

    public void deleteResource(Long id) {
        ResourceEntity current = getResource(id);
        ResourceEntity deleted = new ResourceEntity(
                current.id(),
                current.resourceCode(),
                current.resourceName(),
                current.resourceType(),
                current.sourceType(),
                current.description(),
                current.status(),
                current.createdAt(),
                LocalDateTime.now(),
                true
        );
        resources.put(id, deleted);
    }

    public List<ResourceVersionEntity> listVersions(Long resourceId) {
        getResource(resourceId);
        return versions.values().stream()
                .filter(version -> version.resourceId().equals(resourceId))
                .sorted(Comparator.comparing(ResourceVersionEntity::createdAt).reversed())
                .toList();
    }

    public ResourceVersionEntity createVersion(Long resourceId, CreateResourceVersionRequest request) {
        ResourceEntity resource = getResource(resourceId);
        if (DISABLED.equals(resource.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源已禁用，不能新增版本");
        }
        ensureVersionUnique(resourceId, request.version());
        validateVersionSource(resource, request);
        ResourceVersionEntity version = new ResourceVersionEntity(
                versionIdSequence.getAndIncrement(),
                resourceId,
                request.version(),
                request.externalUrl(),
                request.imageRepository(),
                request.imageTag(),
                request.checksum(),
                request.releaseNote(),
                defaultStatus(request.status()),
                LocalDateTime.now()
        );
        versions.put(version.id(), version);
        return version;
    }

    private void ensureResourceCodeUnique(String resourceCode) {
        boolean exists = resources.values().stream()
                .anyMatch(resource -> resource.resourceCode().equals(resourceCode));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源编码已存在");
        }
    }

    private void ensureVersionUnique(Long resourceId, String version) {
        boolean exists = versions.values().stream()
                .anyMatch(item -> item.resourceId().equals(resourceId) && item.version().equals(version));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源版本已存在");
        }
    }

    private void validateVersionSource(ResourceEntity resource, CreateResourceVersionRequest request) {
        if (resource.resourceType() == ResourceType.IMAGE || resource.sourceType() == ResourceSourceType.HARBOR) {
            if (!StringUtils.hasText(request.imageRepository()) || !StringUtils.hasText(request.imageTag())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "镜像资源版本必须填写镜像仓库和镜像标签");
            }
            return;
        }
        if (!StringUtils.hasText(request.externalUrl()) && !StringUtils.hasText(request.checksum())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非镜像资源版本至少填写外部地址或 checksum");
        }
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status : ENABLED;
    }
}
