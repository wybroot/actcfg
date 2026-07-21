package com.example.delivery.packagebuild;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.audit.AuditService;
import com.example.delivery.common.api.ApiResponse;
import com.example.delivery.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/packages")
public class PackageBuildController {
    private final PackageBuildService packageBuildService;
    private final AuditService auditService;

    public PackageBuildController(PackageBuildService packageBuildService,
                                  AuditService auditService) {
        this.packageBuildService = packageBuildService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<PackageBuildEntity>> listPackages() {
        return ApiResponse.ok(packageBuildService.listPackages());
    }

    @PostMapping("/build")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "PACKAGE", action = "BUILD")
    public ApiResponse<PackageBuildEntity> createPackage(@Valid @RequestBody CreatePackageBuildRequest request) {
        return ApiResponse.ok(packageBuildService.createPackage(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PackageBuildEntity> getPackage(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getPackage(id));
    }

    @GetMapping("/{id}/manifest")
    public ApiResponse<PackageManifest> getManifest(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getManifest(id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<PackageBuildStatus> getStatus(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getStatus(id));
    }

    @GetMapping("/{id}/execution-plan")
    public ApiResponse<com.example.delivery.agent.executor.ExecutionPlan> getExecutionPlan(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getExecutionPlan(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS','IMPL_ENGINEER')")
    public ApiResponse<PackageDownloadInfo> getDownloadInfo(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser,
            HttpServletRequest request
    ) {
        PackageDownloadInfo info = packageBuildService.getDownloadInfo(id);
        PackageBuildEntity pkg = packageBuildService.getPackage(id);
        // 记录下载审计日志
        String username = currentUser != null ? currentUser.username() : "anonymous";
        String ip = request.getRemoteAddr();
        auditService.recordDownload(username, "PACKAGE", pkg.packageCode(),
                pkg.customerId(), pkg.environmentId(), null, ip);
        return ApiResponse.ok(info);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "PACKAGE", action = "DELETE")
    public ApiResponse<Void> deletePackage(@PathVariable Long id) {
        packageBuildService.deletePackage(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "PACKAGE", action = "ARCHIVE")
    public ApiResponse<PackageBuildEntity> archivePackage(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.archivePackage(id));
    }

    @PutMapping("/{id}/deprecate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "PACKAGE", action = "DEPRECATE")
    public ApiResponse<PackageBuildEntity> deprecatePackage(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.deprecatePackage(id));
    }

    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(module = "PACKAGE", action = "CLEANUP")
    public ApiResponse<Integer> cleanupExpired() {
        return ApiResponse.ok(packageBuildService.cleanupExpired());
    }
}
