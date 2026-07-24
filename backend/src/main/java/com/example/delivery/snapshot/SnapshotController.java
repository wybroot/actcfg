package com.example.delivery.snapshot;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SnapshotController {
    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /** 查询某环境的当前配置快照及其组件。 */
    @GetMapping("/environments/{environmentId}/snapshot")
    public ApiResponse<SnapshotDetail> getSnapshot(@PathVariable Long environmentId) {
        SnapshotEntity snapshot = snapshotService.getByEnvironment(environmentId);
        List<SnapshotComponentEntity> components = snapshotService.listComponents(snapshot.id());
        return ApiResponse.ok(new SnapshotDetail(snapshot, components));
    }

    /** 编辑快照组件的配置模板（与源方案解耦）。 */
    @PutMapping("/snapshots/{snapshotId}/components/{componentId}/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "SNAPSHOT", action = "UPDATE_CONFIG")
    public ApiResponse<SnapshotComponentEntity> updateComponentConfig(
            @PathVariable Long snapshotId,
            @PathVariable Long componentId,
            @Valid @RequestBody UpdateConfigRequest request
    ) {
        return ApiResponse.ok(snapshotService.updateComponentConfig(componentId, request.configTemplate()));
    }

    public record SnapshotDetail(SnapshotEntity snapshot, List<SnapshotComponentEntity> components) {}
    public record UpdateConfigRequest(@NotNull String configTemplate) {}
}
