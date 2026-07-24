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
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<List<CustomerEntity>> listCustomers() {
        return ApiResponse.ok(customerService.listCustomers());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerEntity> getCustomer(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getCustomer(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "CUSTOMER", action = "CREATE")
    public ApiResponse<CustomerEntity> createCustomer(@Valid @RequestBody CreateCustomerRequest req) {
        return ApiResponse.ok(customerService.createCustomer(
                req.customerCode(), req.customerName(), req.shortName(), req.industry()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "CUSTOMER", action = "UPDATE")
    public ApiResponse<CustomerEntity> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest req
    ) {
        return ApiResponse.ok(customerService.updateCustomer(id, req.customerName(), req.shortName(), req.industry()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "CUSTOMER", action = "DELETE")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/environments")
    public ApiResponse<List<CustomerEnvironmentEntity>> listEnvironments(@PathVariable Long id) {
        return ApiResponse.ok(customerService.listEnvironments(id));
    }

    @PostMapping("/{id}/environments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS')")
    @AuditLog(module = "CUSTOMER", action = "CREATE_ENV")
    public ApiResponse<CustomerEnvironmentEntity> createEnvironment(
            @PathVariable Long id,
            @Valid @RequestBody CreateEnvironmentRequest req) {
        return ApiResponse.ok(customerService.createEnvironment(id, req.environmentName(), req.environmentType()));
    }

    @GetMapping("/environments/{environmentId}/variables")
    public ApiResponse<List<EnvVariableEntity>> listVariables(@PathVariable Long environmentId) {
        return ApiResponse.ok(customerService.listVariables(environmentId));
    }

    // ---- 内部 DTO ----
    public record CreateCustomerRequest(
            @NotBlank String customerCode,
            @NotBlank String customerName,
            String shortName,
            String industry) {}

    public record UpdateCustomerRequest(
            @NotBlank String customerName,
            String shortName,
            String industry) {}

    public record CreateEnvironmentRequest(
            @NotBlank String environmentName,
            @NotNull EnvironmentType environmentType) {}
}

