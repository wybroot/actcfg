package com.example.delivery.customer;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}/environments")
    public ApiResponse<List<CustomerEnvironmentEntity>> listEnvironments(@PathVariable Long id) {
        return ApiResponse.ok(customerService.listEnvironments(id));
    }

    @GetMapping("/environments/{environmentId}/variables")
    public ApiResponse<List<EnvVariableEntity>> listVariables(@PathVariable Long environmentId) {
        return ApiResponse.ok(customerService.listVariables(environmentId));
    }
}
