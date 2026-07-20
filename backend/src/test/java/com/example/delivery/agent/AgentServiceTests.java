package com.example.delivery.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.packagebuild.PackageBuildService;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentServiceTests {
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        ResourceService resourceService = new ResourceService();
        DeployPlanService deployPlanService = new DeployPlanService(resourceService);
        CustomerService customerService = new CustomerService(deployPlanService);
        PackageBuildService packageBuildService = new PackageBuildService(customerService, deployPlanService, resourceService);
        agentService = new AgentService(packageBuildService);
    }

    @Test
    void createTaskSuccess() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));

        assertEquals(AgentTaskStatus.PENDING, task.taskStatus());
        assertEquals(1L, task.packageBuildId());
        assertTrue(agentService.listExecutionLogs(task.id()).stream()
                .anyMatch(log -> log.stepCode().equals("CREATE_TASK")));
    }

    @Test
    void reportRunningAndSuccessStatus() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));

        AgentTaskEntity running = agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.RUNNING,
                "正在执行",
                "DEPLOY",
                "执行部署",
                "INFO",
                "deploy started"
        ));
        AgentTaskEntity success = agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.SUCCESS,
                "执行成功",
                "HEALTH_CHECK",
                "健康检查",
                "INFO",
                "health check passed"
        ));

        assertEquals(AgentTaskStatus.RUNNING, running.taskStatus());
        assertEquals(AgentTaskStatus.SUCCESS, success.taskStatus());
        assertEquals(3, agentService.listExecutionLogs(task.id()).size());
    }

    @Test
    void cancelTaskSuccess() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));

        AgentTaskEntity canceled = agentService.cancelTask(task.id());

        assertEquals(AgentTaskStatus.CANCELED, canceled.taskStatus());
    }

    @Test
    void reportFinishedTaskRejected() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));
        agentService.cancelTask(task.id());

        BusinessException exception = assertThrows(BusinessException.class, () -> agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.RUNNING,
                "继续执行",
                "DEPLOY",
                "执行部署",
                "INFO",
                "deploy started"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void importReportSuccess() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));
        agentService.cancelTask(task.id());

        AgentExecutionReportEntity report = agentService.importReport(task.id(), new ImportAgentReportRequest(
                task.id(),
                "host-01",
                null,
                null,
                "OK",
                "部署报告内容"
        ));

        assertEquals(task.id(), report.taskId());
        assertEquals(AgentTaskStatus.CANCELED, report.executionStatus());
        assertEquals(report.reportCode(), agentService.getReport(task.id()).reportCode());
        assertTrue(agentService.listReports().stream().anyMatch(item -> item.taskId().equals(task.id())));
    }

    @Test
    void importReportRejectedForUnfinishedTask() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));

        BusinessException exception = assertThrows(BusinessException.class, () -> agentService.importReport(task.id(), new ImportAgentReportRequest(
                task.id(), "host-01", null, null, "OK", "报告"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void importReportRejectedWhenDuplicated() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));
        agentService.cancelTask(task.id());
        agentService.importReport(task.id(), new ImportAgentReportRequest(task.id(), "host-01", null, null, "OK", "报告"));

        BusinessException exception = assertThrows(BusinessException.class, () -> agentService.importReport(task.id(), new ImportAgentReportRequest(
                task.id(), "host-01", null, null, "OK", "报告"
        )));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void retryFailedTaskSuccess() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));
        agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.FAILED, "部署失败", "DEPLOY", "执行部署", "ERROR", "connection refused"
        ));

        AgentTaskEntity retried = agentService.retryTask(task.id());

        assertEquals(AgentTaskStatus.RETRYING, retried.taskStatus());
        assertTrue(agentService.listRetryRecords().stream().anyMatch(view -> view.taskId().equals(task.id())
                && "DEPLOY".equals(view.failedStep())));
    }

    @Test
    void retryNonFailedTaskRejected() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));

        BusinessException exception = assertThrows(BusinessException.class, () -> agentService.retryTask(task.id()));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void retryRecordClosesWhenTaskFinishes() {
        AgentTaskEntity task = agentService.createTask(new CreateAgentTaskRequest(1L, "OFFLINE_DEPLOY"));
        agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.FAILED, "部署失败", "DEPLOY", "执行部署", "ERROR", "connection refused"
        ));
        agentService.retryTask(task.id());

        agentService.reportStatus(task.id(), new ReportAgentStatusRequest(
                AgentTaskStatus.SUCCESS, "续跑成功", "DEPLOY", "执行部署", "INFO", "deploy finished"
        ));

        AgentRetryRecordView view = agentService.listRetryRecords().stream()
                .filter(item -> item.taskId().equals(task.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(AgentTaskStatus.SUCCESS, view.finalStatus());
    }
}
