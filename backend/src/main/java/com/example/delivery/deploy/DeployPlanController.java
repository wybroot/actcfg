package com.example.delivery.deploy;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    public ApiResponse<DeployPlanEntity> createPlan(@Valid @RequestBody CreateDeployPlanRequest request) {
        return ApiResponse.ok(deployPlanService.createPlan(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeployPlanEntity> getPlan(@PathVariable Long id) {
        return ApiResponse.ok(deployPlanService.getPlan(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<DeployPlanVersionEntity>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(deployPlanService.listVersions(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    public ApiResponse<DeployPlanVersionEntity> createVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateDeployPlanVersionRequest request
    ) {
        return ApiResponse.ok(deployPlanService.createVersion(id, request));
    }

    @PostMapping("/versions/{versionId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    public ApiResponse<DeployPlanVersionEntity> publishVersion(@PathVariable Long versionId) {
        return ApiResponse.ok(deployPlanService.publishVersion(versionId));
    }

    @GetMapping("/versions/{versionId}/components")
    public ApiResponse<List<DeployComponentEntity>> listComponents(@PathVariable Long versionId) {
        return ApiResponse.ok(deployPlanService.listComponents(versionId));
    }

    @PostMapping("/versions/{versionId}/components")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    public ApiResponse<DeployComponentEntity> createComponent(
            @PathVariable Long versionId,
            @Valid @RequestBody CreateDeployComponentRequest request
    ) {
        return ApiResponse.ok(deployPlanService.createComponent(versionId, request));
    }
}
