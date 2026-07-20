package com.example.delivery.repository;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ApiResponse<List<ResourceEntity>> listResources() {
        return ApiResponse.ok(resourceService.listResources());
    }

    @PostMapping
    public ApiResponse<ResourceEntity> createResource(@Valid @RequestBody CreateResourceRequest request) {
        return ApiResponse.ok(resourceService.createResource(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceEntity> getResource(@PathVariable Long id) {
        return ApiResponse.ok(resourceService.getResource(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceEntity> updateResource(@PathVariable Long id, @Valid @RequestBody UpdateResourceRequest request) {
        return ApiResponse.ok(resourceService.updateResource(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<ResourceVersionEntity>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(resourceService.listVersions(id));
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<ResourceVersionEntity> createVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateResourceVersionRequest request
    ) {
        return ApiResponse.ok(resourceService.createVersion(id, request));
    }
}
