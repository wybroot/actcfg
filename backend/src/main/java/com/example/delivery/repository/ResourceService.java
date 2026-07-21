package com.example.delivery.repository;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.storage.ObjectStorageService;
import com.example.delivery.storage.StoredObject;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ResourceService {
    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";

    private final ResourceRepository resourceRepository;
    private final ObjectStorageService objectStorageService;
    private final AtomicLong resourceIdSequence = new AtomicLong(1);
    private final AtomicLong versionIdSequence = new AtomicLong(1);
    private final Map<Long, ResourceEntity> resources = new ConcurrentHashMap<>();
    private final Map<Long, ResourceVersionEntity> versions = new ConcurrentHashMap<>();

    @Autowired
    public ResourceService(ObjectProvider<ResourceRepository> resourceRepositoryProvider,
                           ObjectStorageService objectStorageService) {
        this.resourceRepository = resourceRepositoryProvider.getIfAvailable();
        this.objectStorageService = objectStorageService;
        if (this.resourceRepository == null) {
            seed();
        }
    }

    public ResourceService() {
        this.resourceRepository = null;
        this.objectStorageService = null;
        seed();
    }

    public List<ResourceEntity> listResources() {
        if (useJdbc()) {
            return resourceRepository.findAllActive();
        }
        return resources.values().stream()
                .filter(resource -> !resource.deleted())
                .sorted(Comparator.comparing(ResourceEntity::createdAt).reversed())
                .toList();
    }

    public ResourceEntity getResource(Long id) {
        if (useJdbc()) {
            return resourceRepository.findActiveById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资源不存在"));
        }
        ResourceEntity resource = resources.get(id);
        if (resource == null || resource.deleted()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    @Transactional
    public ResourceEntity createResource(CreateResourceRequest request) {
        ensureResourceCodeUnique(request.resourceCode());
        if (useJdbc()) {
            return resourceRepository.insertResource(request, defaultStatus(request.status()));
        }
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

    @Transactional
    public ResourceEntity updateResource(Long id, UpdateResourceRequest request) {
        ResourceEntity current = getResource(id);
        if (useJdbc()) {
            return resourceRepository.updateResource(id, request);
        }
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

    @Transactional
    public void deleteResource(Long id) {
        ResourceEntity current = getResource(id);
        if (useJdbc()) {
            resourceRepository.softDelete(id);
            return;
        }
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
        if (useJdbc()) {
            return resourceRepository.findVersionsByResourceId(resourceId);
        }
        return versions.values().stream()
                .filter(version -> version.resourceId().equals(resourceId))
                .sorted(Comparator.comparing(ResourceVersionEntity::createdAt).reversed())
                .toList();
    }

    @Transactional
    public ResourceVersionEntity createVersion(Long resourceId, CreateResourceVersionRequest request) {
        ResourceEntity resource = getResource(resourceId);
        if (DISABLED.equals(resource.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源已禁用，不能新增版本");
        }
        ensureVersionUnique(resourceId, request.version());
        validateVersionSource(resource, request);
        if (useJdbc()) {
            return resourceRepository.insertVersion(resourceId, request, defaultStatus(request.status()));
        }
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

    /**
     * 上传制品文件并创建资源版本。文件写入对象存储后，externalUrl 记录访问地址，checksum 记录 SHA-256。
     */
    @Transactional
    public ResourceVersionEntity uploadVersion(Long resourceId, String version, String releaseNote,
                                               String status, byte[] fileBytes, String originalFilename,
                                               String contentType) {
        ResourceEntity resource = getResource(resourceId);
        if (DISABLED.equals(resource.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源已禁用，不能新增版本");
        }
        if (!StringUtils.hasText(version)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "版本号不能为空");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        ensureVersionUnique(resourceId, version);
        if (objectStorageService == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "对象存储不可用");
        }

        String checksum = sha256Hex(fileBytes);
        String safeName = originalFilename != null ? originalFilename.replaceAll("[^A-Za-z0-9._-]", "_") : "artifact";
        String objectName = resource.resourceCode() + "/" + version + "/" + safeName;
        StoredObject stored = objectStorageService.putResourceFile(
                objectName, new java.io.ByteArrayInputStream(fileBytes), fileBytes.length, contentType, checksum);

        CreateResourceVersionRequest request = new CreateResourceVersionRequest(
                version, stored.objectUrl(), null, null, checksum, releaseNote, defaultStatus(status));

        if (useJdbc()) {
            return resourceRepository.insertVersion(resourceId, request, defaultStatus(status));
        }
        ResourceVersionEntity created = new ResourceVersionEntity(
                versionIdSequence.getAndIncrement(),
                resourceId, version, stored.objectUrl(), null, null, checksum,
                releaseNote, defaultStatus(status), LocalDateTime.now());
        versions.put(created.id(), created);
        return created;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "校验和计算失败");
        }
    }

    public ResourceVersionEntity getVersion(Long versionId) {
        if (useJdbc()) {
            return resourceRepository.findVersionById(versionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资源版本不存在"));
        }
        ResourceVersionEntity version = versions.get(versionId);
        if (version == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源版本不存在");
        }
        return version;
    }

    public ResourceVersionEntity requireEnabledVersion(Long versionId) {
        ResourceVersionEntity version = getVersion(versionId);
        if (!ENABLED.equals(version.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源版本不可用");
        }
        return version;
    }

    private void ensureResourceCodeUnique(String resourceCode) {
        boolean exists = useJdbc()
                ? resourceRepository.existsByCode(resourceCode)
                : resources.values().stream().anyMatch(resource -> resource.resourceCode().equals(resourceCode));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "资源编码已存在");
        }
    }

    private void ensureVersionUnique(Long resourceId, String version) {
        boolean exists = useJdbc()
                ? resourceRepository.existsVersion(resourceId, version)
                : versions.values().stream().anyMatch(item -> item.resourceId().equals(resourceId) && item.version().equals(version));
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

    private boolean useJdbc() {
        return resourceRepository != null;
    }

    private void seed() {
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
}
