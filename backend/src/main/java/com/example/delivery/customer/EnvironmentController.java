package com.example.delivery.customer;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ApiResponse<CustomerEnvironmentEntity> bindPlan(
            @PathVariable Long id,
            @Valid @RequestBody BindDeployPlanRequest request
    ) {
        return ApiResponse.ok(customerService.bindDeployPlan(id, request));
    }

    @GetMapping("/{id}/variables")
    public ApiResponse<List<EnvVariableEntity>> listVariables(@PathVariable Long id) {
        return ApiResponse.ok(customerService.listVariables(id));
    }
}
