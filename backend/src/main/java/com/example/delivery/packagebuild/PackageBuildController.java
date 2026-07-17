package com.example.delivery.packagebuild;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/manifest")
    public ApiResponse<PackageManifest> getManifest(@PathVariable Long id) {
        return ApiResponse.ok(packageBuildService.getManifest(id));
    }
}
