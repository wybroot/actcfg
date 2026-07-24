package com.example.delivery.repository.source;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 源仓库管理接口。密码在响应中一律脱敏；写操作限 SUPER_ADMIN / OPS。
 */
@RestController
@RequestMapping("/api/repository/sources")
public class SourceRepositoryController {
    private final SourceRepositoryService service;

    public SourceRepositoryController(SourceRepositoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SourceRepositoryEntity>> list() {
        return ApiResponse.ok(service.listRepositories());
    }

    @GetMapping("/{id}")
    public ApiResponse<SourceRepositoryEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getRepository(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "SOURCE_REPO", action = "CREATE")
    public ApiResponse<SourceRepositoryEntity> create(@Valid @RequestBody CreateSourceRepositoryRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "SOURCE_REPO", action = "UPDATE")
    public ApiResponse<SourceRepositoryEntity> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSourceRepositoryRequest request
    ) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "SOURCE_REPO", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "SOURCE_REPO", action = "TEST_CONNECTION")
    public ApiResponse<SourceRepositoryService.TestResult> testConnection(@PathVariable Long id) {
        return ApiResponse.ok(service.testConnection(id));
    }
}
