package com.example.delivery.customer;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    public List<CustomerEntity> listCustomers() {
        return List.of(new CustomerEntity(1L, "CUST001", "示例客户", "示例", "政企", "ENABLED"));
    }

    public List<CustomerEnvironmentEntity> listEnvironments(Long customerId) {
        return List.of(new CustomerEnvironmentEntity(1L, customerId, "生产环境", EnvironmentType.PROD, 1L, "ENABLED"));
    }

    public List<EnvVariableEntity> listVariables(Long environmentId) {
        return List.of(new EnvVariableEntity(1L, environmentId, "db.password", "******", true));
    }
}
