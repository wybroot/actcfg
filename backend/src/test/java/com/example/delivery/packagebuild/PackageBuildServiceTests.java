package com.example.delivery.packagebuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.CreateDeployPlanVersionRequest;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PackageBuildServiceTests {
    private DeployPlanService deployPlanService;
    private PackageBuildService packageBuildService;

    @BeforeEach
    void setUp() {
        ResourceService resourceService = new ResourceService();
        deployPlanService = new DeployPlanService(resourceService);
        CustomerService customerService = new CustomerService(deployPlanService);
        packageBuildService = new PackageBuildService(customerService, deployPlanService, resourceService);
    }

    @Test
    void createPackageBuildSuccess() {
        PackageBuildEntity packageBuild = packageBuildService.createPackage(new CreatePackageBuildRequest(
                1L,
                1L,
                1L,
                "1.0.1",
                "测试部署包"
        ));

        assertEquals(PackageBuildStatus.SUCCESS, packageBuild.buildStatus());
        assertTrue(packageBuild.immutable());
        assertEquals(64, packageBuild.checksum().length());
        assertEquals(packageBuild.checksum(), packageBuildService.getManifest(packageBuild.id()).checksum());
        assertTrue(packageBuildService.getManifest(packageBuild.id()).manifestJson().contains("\"components\""));
    }

    @Test
    void getStatusAndDownloadInfoSuccess() {
        PackageBuildEntity packageBuild = packageBuildService.createPackage(new CreatePackageBuildRequest(
                1L,
                1L,
                1L,
                "1.0.2",
                "下载信息"
        ));

        PackageDownloadInfo downloadInfo = packageBuildService.getDownloadInfo(packageBuild.id());

        assertEquals(PackageBuildStatus.SUCCESS, packageBuildService.getStatus(packageBuild.id()));
        assertEquals(packageBuild.packageCode(), downloadInfo.packageCode());
        assertEquals(packageBuild.filePath(), downloadInfo.filePath());
        assertEquals(packageBuild.checksum(), downloadInfo.checksum());
    }

    @Test
    void deletePackageSuccess() {
        PackageBuildEntity packageBuild = packageBuildService.createPackage(new CreatePackageBuildRequest(
                1L,
                1L,
                1L,
                "1.0.3",
                "删除部署包"
        ));

        packageBuildService.deletePackage(packageBuild.id());

        BusinessException exception = assertThrows(BusinessException.class, () -> packageBuildService.getPackage(packageBuild.id()));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createPackageWithDraftVersionRejected() {
        DeployPlanVersionEntity draft = deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("4.0.0"));

        BusinessException exception = assertThrows(BusinessException.class, () -> packageBuildService.createPackage(new CreatePackageBuildRequest(
                1L,
                1L,
                draft.id(),
                "4.0.0",
                "草稿版本"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void createPackageWithUnboundVersionRejected() {
        DeployPlanVersionEntity published = deployPlanService.publishVersion(
                deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("4.1.0")).id()
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> packageBuildService.createPackage(new CreatePackageBuildRequest(
                1L,
                1L,
                published.id(),
                "4.1.0",
                "未绑定版本"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }
}
