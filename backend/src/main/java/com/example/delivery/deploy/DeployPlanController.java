package com.example.delivery.deploy;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deploy/plans")
public class DeployPlanController {
    private final DeployPlanService deployPlanService;

    public DeployPlanController(DeployPlanService deployPlanService) {
        this.deployPlanService = deployPlanService;
    }

    @GetMapping
    public ApiResponse<List<DeployPlanEntity>> listPlans() {
        return ApiResponse.ok(deployPlanService.listPlans());
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<DeployPlanVersionEntity>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(deployPlanService.listVersions(id));
    }

    @GetMapping("/versions/{versionId}/components")
    public ApiResponse<List<DeployComponentEntity>> listComponents(@PathVariable Long versionId) {
        return ApiResponse.ok(deployPlanService.listComponents(versionId));
    }
}
