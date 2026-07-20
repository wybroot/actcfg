package com.example.delivery.packagebuild;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/packages")
public class PackageBuildController {
    private final PackageBuildService packageBuildService;

    public PackageBuildController(PackageBuildService packageBuildService) {
        this.packageBuildService = packageBuildService;
    }

    @GetMapping
    public ApiResponse<List<PackageBuildEntity>> listPackages() {
        return ApiResponse.ok(packageBuildService.listPackages());
    }

    @PostMapping("/build")
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

    @GetMapping("/{id}/download")
    public ApiResponse<PackageDownloadInfo> getDownloadInfo(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getDownloadInfo(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePackage(@PathVariable Long id) {
        packageBuildService.deletePackage(id);
        return ApiResponse.ok();
    }
}
