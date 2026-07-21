package com.example.delivery.repository;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.common.api.ApiResponse;
import com.example.delivery.repository.harbor.HarborSyncRequest;
import com.example.delivery.repository.harbor.HarborSyncService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/repository/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final HarborSyncService harborSyncService;

    public ResourceController(ResourceService resourceService, HarborSyncService harborSyncService) {
        this.resourceService = resourceService;
        this.harborSyncService = harborSyncService;
    }

    @GetMapping
    public ApiResponse<List<ResourceEntity>> listResources() {
        return ApiResponse.ok(resourceService.listResources());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "CREATE")
    public ApiResponse<ResourceEntity> createResource(@Valid @RequestBody CreateResourceRequest request) {
        return ApiResponse.ok(resourceService.createResource(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceEntity> getResource(@PathVariable Long id) {
        return ApiResponse.ok(resourceService.getResource(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "UPDATE")
    public ApiResponse<ResourceEntity> updateResource(@PathVariable Long id, @Valid @RequestBody UpdateResourceRequest request) {
        return ApiResponse.ok(resourceService.updateResource(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "DELETE")
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<ResourceVersionEntity>> listVersions(@PathVariable Long id) {
        return ApiResponse.ok(resourceService.listVersions(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "CREATE_VERSION")
    public ApiResponse<ResourceVersionEntity> createVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateResourceVersionRequest request
    ) {
        return ApiResponse.ok(resourceService.createVersion(id, request));
    }

    @PostMapping(value = "/{id}/versions/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "UPLOAD_VERSION")
    public ApiResponse<ResourceVersionEntity> uploadVersion(
            @PathVariable Long id,
            @RequestParam("version") String version,
            @RequestParam(value = "releaseNote", required = false) String releaseNote,
            @RequestParam(value = "status", required = false) String status,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ApiResponse.ok(resourceService.uploadVersion(
                id, version, releaseNote, status,
                file.getBytes(), file.getOriginalFilename(), file.getContentType()));
    }

    @PostMapping("/{id}/versions/harbor-sync")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "RESOURCE", action = "HARBOR_SYNC")
    public ApiResponse<ResourceVersionEntity> harborSync(
            @PathVariable Long id,
            @Valid @RequestBody HarborSyncRequest request
    ) {
        return ApiResponse.ok(harborSyncService.sync(id, request));
    }
}
