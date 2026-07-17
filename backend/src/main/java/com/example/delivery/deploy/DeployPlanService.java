package com.example.delivery.deploy;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeployPlanService {

    public List<DeployPlanEntity> listPlans() {
        return List.of(new DeployPlanEntity(1L, "PLAN-001", "标准单机部署方案", 1L, "ENABLED", LocalDateTime.now()));
    }

    public List<DeployPlanVersionEntity> listVersions(Long planId) {
        return List.of(new DeployPlanVersionEntity(1L, planId, "1.0.0", DeployPlanVersionStatus.PUBLISHED, false, LocalDateTime.now()));
    }

    public List<DeployComponentEntity> listComponents(Long versionId) {
        return List.of(new DeployComponentEntity(1L, versionId, "应用服务", "APP", 1L, 1, "server.port=${app.port}", "GET /actuator/health"));
    }
}
