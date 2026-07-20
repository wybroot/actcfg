package com.example.delivery.customer;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.deploy.DeployPlanVersionStatus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final DeployPlanService deployPlanService;
    private final CustomerRepository customerRepository;
    private final AtomicLong customerIdSequence = new AtomicLong(1);
    private final AtomicLong environmentIdSequence = new AtomicLong(1);
    private final AtomicLong variableIdSequence = new AtomicLong(1);
    private final Map<Long, CustomerEntity> customers = new ConcurrentHashMap<>();
    private final Map<Long, CustomerEnvironmentEntity> environments = new ConcurrentHashMap<>();
    private final Map<Long, EnvVariableEntity> variables = new ConcurrentHashMap<>();

    @Autowired
    public CustomerService(DeployPlanService deployPlanService, ObjectProvider<CustomerRepository> customerRepositoryProvider) {
        this.deployPlanService = deployPlanService;
        this.customerRepository = customerRepositoryProvider.getIfAvailable();
        if (this.customerRepository == null) {
            seed();
        }
    }

    public CustomerService(DeployPlanService deployPlanService) {
        this.deployPlanService = deployPlanService;
        this.customerRepository = null;
        seed();
    }

    public List<CustomerEntity> listCustomers() {
        if (useJdbc()) {
            return customerRepository.findAllActiveCustomers();
        }
        return customers.values().stream().toList();
    }

    public CustomerEntity getCustomer(Long id) {
        if (useJdbc()) {
            return customerRepository.findActiveCustomerById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "客户不存在"));
        }
        CustomerEntity customer = customers.get(id);
        if (customer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        return customer;
    }

    public List<CustomerEnvironmentEntity> listEnvironments(Long customerId) {
        getCustomer(customerId);
        if (useJdbc()) {
            return customerRepository.findEnvironmentsByCustomerId(customerId);
        }
        return environments.values().stream()
                .filter(environment -> environment.customerId().equals(customerId))
                .toList();
    }

    public CustomerEnvironmentEntity getEnvironment(Long environmentId) {
        if (useJdbc()) {
            return customerRepository.findEnvironmentById(environmentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "客户环境不存在"));
        }
        CustomerEnvironmentEntity environment = environments.get(environmentId);
        if (environment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户环境不存在");
        }
        return environment;
    }

    @Transactional
    public CustomerEnvironmentEntity bindDeployPlan(Long environmentId, BindDeployPlanRequest request) {
        CustomerEnvironmentEntity current = getEnvironment(environmentId);
        DeployPlanVersionEntity version = deployPlanService.getVersion(request.deployPlanVersionId());
        if (version.status() != DeployPlanVersionStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只能绑定已发布的部署方案版本");
        }
        if (useJdbc()) {
            return customerRepository.updateEnvironmentDeployPlanVersion(environmentId, version.id());
        }
        CustomerEnvironmentEntity updated = new CustomerEnvironmentEntity(
                current.id(),
                current.customerId(),
                current.environmentName(),
                current.environmentType(),
                version.id(),
                current.status()
        );
        environments.put(environmentId, updated);
        return updated;
    }

    public List<EnvVariableEntity> listVariables(Long environmentId) {
        getEnvironment(environmentId);
        if (useJdbc()) {
            return customerRepository.findVariablesByEnvironmentId(environmentId);
        }
        return variables.values().stream()
                .filter(variable -> variable.environmentId().equals(environmentId))
                .toList();
    }

    private boolean useJdbc() {
        return customerRepository != null;
    }

    private void seed() {
        CustomerEntity customer = new CustomerEntity(
                customerIdSequence.getAndIncrement(),
                "CUST001",
                "示例客户",
                "示例",
                "政企",
                "ENABLED"
        );
        customers.put(customer.id(), customer);

        CustomerEnvironmentEntity environment = new CustomerEnvironmentEntity(
                environmentIdSequence.getAndIncrement(),
                customer.id(),
                "生产环境",
                EnvironmentType.PROD,
                1L,
                "ENABLED"
        );
        environments.put(environment.id(), environment);

        EnvVariableEntity variable = new EnvVariableEntity(
                variableIdSequence.getAndIncrement(),
                environment.id(),
                "db.password",
                "******",
                true
        );
        variables.put(variable.id(), variable);
    }
}
