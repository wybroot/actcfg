package com.example.delivery.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.deploy.CreateDeployPlanVersionRequest;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.deploy.DeployPlanVersionEntity;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceTests {
    private DeployPlanService deployPlanService;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        deployPlanService = new DeployPlanService(new ResourceService());
        customerService = new CustomerService(deployPlanService);
    }

    @Test
    void listEnvironmentsSuccess() {
        assertTrue(customerService.listEnvironments(1L).stream()
                .anyMatch(environment -> environment.environmentName().equals("生产环境")));
    }

    @Test
    void bindPublishedDeployPlanVersionSuccess() {
        CustomerEnvironmentEntity updated = customerService.bindDeployPlan(1L, new BindDeployPlanRequest(1L));

        assertEquals(1L, updated.deployPlanVersionId());
        assertEquals(1L, customerService.getEnvironment(1L).deployPlanVersionId());
    }

    @Test
    void bindDraftDeployPlanVersionRejected() {
        DeployPlanVersionEntity draft = deployPlanService.createVersion(1L, new CreateDeployPlanVersionRequest("3.0.0"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.bindDeployPlan(1L, new BindDeployPlanRequest(draft.id())));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void bindMissingDeployPlanVersionRejected() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.bindDeployPlan(1L, new BindDeployPlanRequest(999L)));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void bindMissingEnvironmentRejected() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerService.bindDeployPlan(999L, new BindDeployPlanRequest(1L)));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void sensitiveVariableValueIsMaskedOnRead() {
        customerService.createVariable(1L, "DB_PASS", "secret123", true);

        EnvVariableEntity masked = customerService.listVariables(1L).stream()
                .filter(v -> v.variableKey().equals("DB_PASS"))
                .findFirst().orElseThrow();

        // 对外读取：明文值必须被抹掉，只保留掩码
        assertEquals("", masked.variableValue());
        assertNotEquals("secret123", masked.variableValue());
        assertEquals("******", masked.maskedValue());
        assertTrue(masked.sensitive());
    }

    @Test
    void nonSensitiveVariableKeepsPlainValue() {
        customerService.createVariable(1L, "DB_HOST", "10.0.0.5", false);

        EnvVariableEntity plain = customerService.listVariables(1L).stream()
                .filter(v -> v.variableKey().equals("DB_HOST"))
                .findFirst().orElseThrow();

        assertEquals("10.0.0.5", plain.variableValue());
    }
}
