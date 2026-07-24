package com.example.delivery.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.packagebuild.PackageBuildService;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentInstanceTests {
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
    void registerAndHeartbeatKeepsOnline() {
        AgentInstanceEntity ins = agentService.registerInstance(
                new RegisterAgentRequest("AGENT-01", "host-01", 1L, 1L), "10.0.0.9");

        assertEquals("ONLINE", ins.instanceStatus());
        assertEquals("AGENT-01", ins.agentCode());
        assertNotNull(ins.lastHeartbeatAt());

        AgentInstanceEntity beat = agentService.heartbeat("AGENT-01");
        assertEquals("ONLINE", beat.instanceStatus());

        assertTrue(agentService.listInstances().stream()
                .anyMatch(i -> i.agentCode().equals("AGENT-01")));
    }

    @Test
    void registerIsIdempotentByCode() {
        AgentInstanceEntity first = agentService.registerInstance(
                new RegisterAgentRequest("AGENT-02", "host-a", null, null), "10.0.0.1");
        AgentInstanceEntity second = agentService.registerInstance(
                new RegisterAgentRequest("AGENT-02", "host-b", null, null), "10.0.0.2");

        // 同 code 复用同 id，不新增实例
        assertEquals(first.id(), second.id());
        assertEquals(1, agentService.listInstances().stream()
                .filter(i -> i.agentCode().equals("AGENT-02")).count());
    }

    @Test
    void claimNextTaskMovesPendingToRunning() {
        agentService.registerInstance(new RegisterAgentRequest("AGENT-03", "host", null, null), "10.0.0.3");
        AgentTaskEntity created = agentService.createTask(new CreateAgentTaskRequest(1L, "ONLINE_DEPLOY"));
        assertEquals(AgentTaskStatus.PENDING, created.taskStatus());

        AgentTaskEntity claimed = agentService.claimNextTask("AGENT-03");

        assertNotNull(claimed);
        assertEquals(AgentTaskStatus.RUNNING, claimed.taskStatus());
    }

    @Test
    void claimReturnsNullWhenNoPending() {
        agentService.registerInstance(new RegisterAgentRequest("AGENT-04", "host", null, null), "10.0.0.4");
        // seed 任务是 SUCCESS，无 PENDING
        AgentTaskEntity claimed = agentService.claimNextTask("AGENT-04");
        assertNull(claimed);
    }
}
