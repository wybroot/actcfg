package com.example.delivery.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.delivery.agent.AgentService;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.deploy.DeployPlanService;
import com.example.delivery.packagebuild.PackageBuildService;
import com.example.delivery.repository.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatsServiceTests {
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        ResourceService resourceService = new ResourceService();
        DeployPlanService deployPlanService = new DeployPlanService(resourceService);
        CustomerService customerService = new CustomerService(deployPlanService);
        PackageBuildService packageBuildService = new PackageBuildService(customerService, deployPlanService, resourceService);
        AgentService agentService = new AgentService(packageBuildService);
        statsService = new StatsService(customerService, resourceService, packageBuildService, agentService);
    }

    @Test
    void overviewAggregatesCounts() {
        StatsOverview overview = statsService.overview();

        // seed 数据保证各计数 > 0
        assertTrue(overview.customerCount() > 0);
        assertTrue(overview.resourceCount() > 0);
        assertTrue(overview.packageCount() > 0);
        assertTrue(overview.agentTaskCount() > 0);
        // 任务状态计数之和等于任务总数
        long sum = overview.taskStatusCounts().values().stream().mapToLong(Long::longValue).sum();
        assertEquals(overview.agentTaskCount(), sum);
    }
}
