package com.example.delivery.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
