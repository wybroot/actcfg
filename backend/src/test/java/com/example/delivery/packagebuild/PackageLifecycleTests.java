package com.example.delivery.packagebuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackageLifecycleTests {
    private PackageBuildService packageBuildService;

    @BeforeEach
    void setUp() {
        ResourceService resourceService = new ResourceService();
        DeployPlanService deployPlanService = new DeployPlanService(resourceService);
        CustomerService customerService = new CustomerService(deployPlanService);
        packageBuildService = new PackageBuildService(customerService, deployPlanService, resourceService);
    }

    private PackageBuildEntity build(String version) {
        return packageBuildService.createPackage(new CreatePackageBuildRequest(1L, 1L, 1L, version, version));
    }

    @Test
    void newPackageIsActive() {
        PackageBuildEntity pkg = build("2.0.0");
        assertEquals(PackageLifecycleState.ACTIVE, pkg.lifecycleState());
        assertEquals(0L, pkg.downloadCount());
    }

    @Test
    void archiveThenStillDownloadable() {
        PackageBuildEntity pkg = build("2.0.1");
        PackageBuildEntity archived = packageBuildService.archivePackage(pkg.id());
        assertEquals(PackageLifecycleState.ARCHIVED, archived.lifecycleState());
        // 归档态仍可下载
        packageBuildService.getDownloadInfo(pkg.id());
        assertEquals(1L, packageBuildService.getPackage(pkg.id()).downloadCount());
    }

    @Test
    void deprecateBlocksDownload() {
        PackageBuildEntity pkg = build("2.0.2");
        packageBuildService.deprecatePackage(pkg.id());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> packageBuildService.getDownloadInfo(pkg.id()));
        assertEquals(ErrorCode.STATE_CONFLICT, ex.getErrorCode());
    }

    @Test
    void downloadIncrementsCount() {
        PackageBuildEntity pkg = build("2.0.3");
        packageBuildService.getDownloadInfo(pkg.id());
        packageBuildService.getDownloadInfo(pkg.id());
        assertEquals(2L, packageBuildService.getPackage(pkg.id()).downloadCount());
    }

    @Test
    void illegalTransitionRejected() {
        PackageBuildEntity pkg = build("2.0.4");
        packageBuildService.deprecatePackage(pkg.id());
        // 废弃态不能再归档
        BusinessException ex = assertThrows(BusinessException.class,
                () -> packageBuildService.archivePackage(pkg.id()));
        assertEquals(ErrorCode.STATE_CONFLICT, ex.getErrorCode());
    }

    @Test
    void cleanupSkipsPackagesWithinRetention() {
        PackageBuildEntity pkg = build("2.0.5");
        packageBuildService.deprecatePackage(pkg.id());
        // 保留期默认 90 天未到，清理不应触及
        assertEquals(0, packageBuildService.cleanupExpired());
        assertEquals(PackageLifecycleState.DEPRECATED, packageBuildService.getPackage(pkg.id()).lifecycleState());
    }
}
