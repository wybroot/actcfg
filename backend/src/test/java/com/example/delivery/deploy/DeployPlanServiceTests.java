package com.example.delivery.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeployPlanServiceTests {
    private DeployPlanService deployPlanService;

    @BeforeEach
    void setUp() {
        deployPlanService = new DeployPlanService();
    }

    @Test
    void createPlanSuccess() {
        DeployPlanEntity created = deployPlanService.createPlan(new CreateDeployPlanRequest(
                "PLAN-002",
                "多环境部署方案",
                "用于测试发布流程"
        ));

        assertEquals("PLAN-002", created.planCode());
        assertEquals("ENABLED", created.status());
    }

    @Test
    void createPlanDuplicateCodeRejected() {
        deployPlanService.createPlan(new CreateDeployPlanRequest("PLAN-002", "多环境部署方案", "说明"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> deployPlanService.createPlan(new CreateDeployPlanRequest("PLAN-002", "重复方案", "说明")));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void createVersionSuccess() {
        DeployPlanVersionEntity version = deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("1.2.0"));

        assertEquals(DeployPlanVersionStatus.DRAFT, version.status());
        assertTrue(version.canEdit());
    }

    @Test
    void publishDraftVersionSuccess() {
        DeployPlanVersionEntity draft = deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("2.0.0"));

        DeployPlanVersionEntity published = deployPlanService.publishVersion(draft.id());

        assertEquals(DeployPlanVersionStatus.PUBLISHED, published.status());
        assertFalse(published.canEdit());
        assertEquals(draft.id(), deployPlanService.getPlan(1L).currentVersionId());
    }

    @Test
    void publishPublishedVersionRejected() {
        BusinessException exception = assertThrows(BusinessException.class, () -> deployPlanService.publishVersion(1L));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void createComponentOnDraftSuccess() {
        DeployPlanVersionEntity draft = deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("2.1.0"));
        DeployComponentEntity component = deployPlanService.createComponent(draft.id(), new CreateDeployComponentRequest(
                "任务服务",
                "APP",
                1L,
                2,
                "server.port=${task.port}",
                "GET /actuator/health"
        ));

        assertEquals(draft.id(), component.planVersionId());
    }

    @Test
    void createComponentOnPublishedVersionRejected() {
        BusinessException exception = assertThrows(BusinessException.class, () -> deployPlanService.createComponent(1L, new CreateDeployComponentRequest(
                "只读服务",
                "APP",
                1L,
                2,
                "server.port=${task.port}",
                "GET /actuator/health"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }
}
