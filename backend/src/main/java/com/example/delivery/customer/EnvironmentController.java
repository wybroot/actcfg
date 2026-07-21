package com.example.delivery.customer;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {
    private final CustomerService customerService;

    public EnvironmentController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerEnvironmentEntity> getEnvironment(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getEnvironment(id));
    }

    @PutMapping("/{id}/bind-plan")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "ENVIRONMENT", action = "BIND_PLAN")
    public ApiResponse<CustomerEnvironmentEntity> bindPlan(
            @PathVariable Long id,
            @Valid @RequestBody BindDeployPlanRequest request
    ) {
        return ApiResponse.ok(customerService.bindDeployPlan(id, request));
    }

    // ---- 环境变量 CRUD ----

    @GetMapping("/{id}/variables")
    public ApiResponse<List<EnvVariableEntity>> listVariables(@PathVariable Long id) {
        return ApiResponse.ok(customerService.listVariables(id));
    }

    @PostMapping("/{id}/variables")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "ENV_VARIABLE", action = "CREATE")
    public ApiResponse<EnvVariableEntity> createVariable(
            @PathVariable Long id,
            @Valid @RequestBody CreateVariableRequest request
    ) {
        return ApiResponse.ok(customerService.createVariable(
                id, request.key(), request.value(), request.sensitive()));
    }

    @PutMapping("/{id}/variables/{variableId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "ENV_VARIABLE", action = "UPDATE")
    public ApiResponse<EnvVariableEntity> updateVariable(
            @PathVariable Long id,
            @PathVariable Long variableId,
            @Valid @RequestBody UpdateVariableRequest request
    ) {
        return ApiResponse.ok(customerService.updateVariable(variableId, request.value(), request.sensitive()));
    }

    @DeleteMapping("/{id}/variables/{variableId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "ENV_VARIABLE", action = "DELETE")
    public ApiResponse<Void> deleteVariable(@PathVariable Long id, @PathVariable Long variableId) {
        customerService.deleteVariable(variableId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/variables/clone-from/{fromId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "ENV_VARIABLE", action = "CLONE")
    public ApiResponse<List<EnvVariableEntity>> cloneVariables(
            @PathVariable Long id,
            @PathVariable Long fromId
    ) {
        return ApiResponse.ok(customerService.cloneVariables(fromId, id));
    }

    /** 密钥轮换：将所有敏感变量重新用当前活跃密钥加密。系统级操作，仅超管。 */
    @PostMapping("/variables/rotate-secrets")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(module = "ENV_VARIABLE", action = "ROTATE_SECRETS")
    public ApiResponse<Integer> rotateSecrets() {
        return ApiResponse.ok(customerService.rotateSecrets());
    }

    // ---- 内部 DTO ----
    public record CreateVariableRequest(@NotBlank String key, String value, boolean sensitive) {}
    public record UpdateVariableRequest(@NotNull String value, boolean sensitive) {}
}

