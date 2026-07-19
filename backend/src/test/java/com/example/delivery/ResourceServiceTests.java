package com.example.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.repository.CreateResourceRequest;
import com.example.delivery.repository.CreateResourceVersionRequest;
import com.example.delivery.repository.ResourceEntity;
import com.example.delivery.repository.ResourceService;
import com.example.delivery.repository.ResourceSourceType;
import com.example.delivery.repository.ResourceType;
import com.example.delivery.repository.UpdateResourceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceServiceTests {
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService();
    }

    @Test
    void createResourceSuccess() {
        ResourceEntity created = resourceService.createResource(new CreateResourceRequest(
                "RES-SCRIPT-001",
                "初始化脚本",
                ResourceType.SCRIPT,
                ResourceSourceType.UPLOAD,
                "现场初始化脚本",
                null
        ));

        assertEquals("ENABLED", created.status());
        assertTrue(resourceService.listResources().stream()
                .anyMatch(resource -> resource.resourceCode().equals("RES-SCRIPT-001")));
    }

    @Test
    void createResourceDuplicateCodeRejected() {
        CreateResourceRequest request = new CreateResourceRequest(
                "RES-SCRIPT-001",
                "初始化脚本",
                ResourceType.SCRIPT,
                ResourceSourceType.UPLOAD,
                "现场初始化脚本",
                null
        );
        resourceService.createResource(request);

        BusinessException exception = assertThrows(BusinessException.class, () -> resourceService.createResource(request));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void getResourceMissingRejected() {
        BusinessException exception = assertThrows(BusinessException.class, () -> resourceService.getResource(999L));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateResourceSuccess() {
        ResourceEntity created = resourceService.createResource(new CreateResourceRequest(
                "RES-SCRIPT-001",
                "初始化脚本",
                ResourceType.SCRIPT,
                ResourceSourceType.UPLOAD,
                "现场初始化脚本",
                null
        ));

        ResourceEntity updated = resourceService.updateResource(created.id(), new UpdateResourceRequest(
                "初始化脚本-更新",
                ResourceType.SCRIPT,
                ResourceSourceType.INTERNAL_REPO,
                "更新后的说明",
                "DISABLED"
        ));

        assertEquals("RES-SCRIPT-001", updated.resourceCode());
        assertEquals("初始化脚本-更新", updated.resourceName());
        assertEquals("DISABLED", updated.status());
    }

    @Test
    void deleteResourceHidesFromList() {
        ResourceEntity created = resourceService.createResource(new CreateResourceRequest(
                "RES-SCRIPT-001",
                "初始化脚本",
                ResourceType.SCRIPT,
                ResourceSourceType.UPLOAD,
                "现场初始化脚本",
                null
        ));

        resourceService.deleteResource(created.id());

        assertFalse(resourceService.listResources().stream()
                .anyMatch(resource -> resource.id().equals(created.id())));
        BusinessException exception = assertThrows(BusinessException.class, () -> resourceService.getResource(created.id()));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createVersionSuccess() {
        ResourceEntity created = resourceService.createResource(new CreateResourceRequest(
                "RES-SCRIPT-001",
                "初始化脚本",
                ResourceType.SCRIPT,
                ResourceSourceType.UPLOAD,
                "现场初始化脚本",
                null
        ));

        resourceService.createVersion(created.id(), new CreateResourceVersionRequest(
                "1.0.0",
                "internal://repo/init.sql",
                null,
                null,
                "sha256-placeholder",
                "初始版本",
                null
        ));

        assertTrue(resourceService.listVersions(created.id()).stream()
                .anyMatch(version -> version.version().equals("1.0.0")));
    }

    @Test
    void createVersionDuplicateVersionRejected() {
        resourceService.createVersion(1L, new CreateResourceVersionRequest(
                "1.0.1",
                "internal://repo/example-app-1.0.1.jar",
                null,
                null,
                "sha256-placeholder",
                "修复版本",
                null
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> resourceService.createVersion(1L, new CreateResourceVersionRequest(
                "1.0.1",
                "internal://repo/example-app-1.0.1.jar",
                null,
                null,
                "sha256-placeholder",
                "重复版本",
                null
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void createImageVersionRequiresRepositoryAndTag() {
        ResourceEntity image = resourceService.createResource(new CreateResourceRequest(
                "RES-IMAGE-001",
                "应用镜像",
                ResourceType.IMAGE,
                ResourceSourceType.HARBOR,
                "Harbor 镜像",
                null
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> resourceService.createVersion(image.id(), new CreateResourceVersionRequest(
                "1.0.0",
                null,
                null,
                null,
                "sha256-placeholder",
                "初始镜像",
                null
        )));
        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
    }
}
