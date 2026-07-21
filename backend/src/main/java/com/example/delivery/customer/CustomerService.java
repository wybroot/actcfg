package com.example.delivery.customer;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.deploy.DeployPlanVersionStatus;
import com.example.delivery.snapshot.SnapshotService;
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
    private final SnapshotService snapshotService;
    private final AtomicLong customerIdSequence = new AtomicLong(1);
    private final AtomicLong environmentIdSequence = new AtomicLong(1);
    private final AtomicLong variableIdSequence = new AtomicLong(1);
    private final Map<Long, CustomerEntity> customers = new ConcurrentHashMap<>();
    private final Map<Long, CustomerEnvironmentEntity> environments = new ConcurrentHashMap<>();
    private final Map<Long, EnvVariableEntity> variables = new ConcurrentHashMap<>();

    @Autowired
    public CustomerService(DeployPlanService deployPlanService,
                           ObjectProvider<CustomerRepository> customerRepositoryProvider,
                           ObjectProvider<SnapshotService> snapshotServiceProvider) {
        this.deployPlanService = deployPlanService;
        this.customerRepository = customerRepositoryProvider.getIfAvailable();
        this.snapshotService = snapshotServiceProvider.getIfAvailable();
        if (this.customerRepository == null) {
            seed();
        }
    }

    public CustomerService(DeployPlanService deployPlanService) {
        this.deployPlanService = deployPlanService;
        this.customerRepository = null;
        this.snapshotService = null;
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
        CustomerEnvironmentEntity updated;
        if (useJdbc()) {
            updated = customerRepository.updateEnvironmentDeployPlanVersion(environmentId, version.id());
        } else {
            updated = new CustomerEnvironmentEntity(
                    current.id(),
                    current.customerId(),
                    current.environmentName(),
                    current.environmentType(),
                    version.id(),
                    current.status()
            );
            environments.put(environmentId, updated);
        }
        // 绑定成功后自动生成独立配置快照（深拷贝方案组件），与源方案解耦
        if (snapshotService != null) {
            snapshotService.createSnapshot(updated.customerId(), environmentId, version.id());
        }
        return updated;
    }

    /** 对外读取：敏感变量的明文值一律抹掉，只保留掩码。 */
    public List<EnvVariableEntity> listVariables(Long environmentId) {
        return listVariablesRaw(environmentId).stream()
                .map(CustomerService::maskSensitive)
                .toList();
    }

    /** 内部读取：保留明文值，仅供克隆等内部操作使用，禁止直接返回给前端。 */
    private List<EnvVariableEntity> listVariablesRaw(Long environmentId) {
        getEnvironment(environmentId);
        if (useJdbc()) {
            return customerRepository.findVariablesByEnvironmentId(environmentId);
        }
        return variables.values().stream()
                .filter(variable -> variable.environmentId().equals(environmentId))
                .toList();
    }

    /** 敏感变量对外脱敏：清空明文值，确保掩码存在。 */
    private static EnvVariableEntity maskSensitive(EnvVariableEntity v) {
        if (!v.sensitive()) {
            return v;
        }
        String masked = v.maskedValue() != null ? v.maskedValue() : "******";
        return new EnvVariableEntity(v.id(), v.environmentId(), v.variableKey(), "", masked, true);
    }

    // ---- 客户增删改 ----

    @Transactional
    public CustomerEntity createCustomer(String customerCode, String customerName,
                                         String shortName, String industry) {
        if (useJdbc()) {
            return customerRepository.insertCustomer(customerCode, customerName, shortName, industry);
        }
        boolean exists = customers.values().stream()
                .anyMatch(c -> c.customerCode().equals(customerCode));
        if (exists) throw new BusinessException(ErrorCode.STATE_CONFLICT, "客户编码已存在");
        CustomerEntity customer = new CustomerEntity(
                customerIdSequence.getAndIncrement(), customerCode, customerName, shortName, industry, "ENABLED");
        customers.put(customer.id(), customer);
        return customer;
    }

    @Transactional
    public CustomerEntity updateCustomer(Long id, String customerName, String shortName, String industry) {
        getCustomer(id);
        if (useJdbc()) {
            return customerRepository.updateCustomer(id, customerName, shortName, industry);
        }
        CustomerEntity current = customers.get(id);
        CustomerEntity updated = new CustomerEntity(
                current.id(), current.customerCode(), customerName, shortName, industry, current.status());
        customers.put(id, updated);
        return updated;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        getCustomer(id);
        if (useJdbc()) {
            customerRepository.softDeleteCustomer(id);
            return;
        }
        customers.remove(id);
    }

    // ---- 环境变量增删改 + 克隆 ----

    @Transactional
    public EnvVariableEntity createVariable(Long environmentId, String key,
                                             String value, boolean sensitive) {
        getEnvironment(environmentId);
        if (useJdbc()) {
            return customerRepository.insertVariable(environmentId, key, value, sensitive);
        }
        boolean exists = variables.values().stream()
                .anyMatch(v -> v.environmentId().equals(environmentId) && v.variableKey().equals(key));
        if (exists) throw new BusinessException(ErrorCode.STATE_CONFLICT, "变量 key 已存在");
        String masked = sensitive ? "******" : null;
        EnvVariableEntity variable = new EnvVariableEntity(
                variableIdSequence.getAndIncrement(), environmentId, key, value, masked, sensitive);
        variables.put(variable.id(), variable);
        return variable;
    }

    @Transactional
    public EnvVariableEntity updateVariable(Long variableId, String value, boolean sensitive) {
        EnvVariableEntity current = getVariable(variableId);
        if (useJdbc()) {
            return customerRepository.updateVariable(variableId, value, sensitive);
        }
        String masked = sensitive ? "******" : null;
        EnvVariableEntity updated = new EnvVariableEntity(
                current.id(), current.environmentId(), current.variableKey(), value, masked, sensitive);
        variables.put(variableId, updated);
        return updated;
    }

    @Transactional
    public void deleteVariable(Long variableId) {
        getVariable(variableId);
        if (useJdbc()) {
            customerRepository.deleteVariable(variableId);
            return;
        }
        variables.remove(variableId);
    }

    /** 将 fromEnvironmentId 的全部变量克隆到 toEnvironmentId（相同 key 则跳过）。 */
    @Transactional
    public List<EnvVariableEntity> cloneVariables(Long fromEnvironmentId, Long toEnvironmentId) {
        getEnvironment(fromEnvironmentId);
        getEnvironment(toEnvironmentId);
        List<EnvVariableEntity> source = listVariablesRaw(fromEnvironmentId);
        List<EnvVariableEntity> existing = listVariablesRaw(toEnvironmentId);
        java.util.Set<String> existingKeys = existing.stream()
                .map(EnvVariableEntity::variableKey).collect(java.util.stream.Collectors.toSet());
        return source.stream()
                .filter(v -> !existingKeys.contains(v.variableKey()))
                .map(v -> createVariable(toEnvironmentId, v.variableKey(), v.variableValue(), v.sensitive()))
                .toList();
    }

    private EnvVariableEntity getVariable(Long variableId) {
        if (useJdbc()) {
            return customerRepository.findVariableById(variableId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "变量不存在"));
        }
        EnvVariableEntity v = variables.get(variableId);
        if (v == null) throw new BusinessException(ErrorCode.NOT_FOUND, "变量不存在");
        return v;
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
                "",       // variableValue（敏感值留空）
                "******",
                true
        );
        variables.put(variable.id(), variable);
    }
}
