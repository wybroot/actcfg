package com.example.delivery.repository;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/versions")
    public ApiResponse<List<ResourceVersionEntity>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(resourceService.listVersions(id));
    }
}
