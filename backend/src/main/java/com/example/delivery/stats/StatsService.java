package com.example.delivery.stats;

import com.example.delivery.agent.AgentService;
import com.example.delivery.agent.AgentTaskEntity;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.packagebuild.PackageBuildService;
import com.example.delivery.repository.ResourceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Dashboard 统计服务：复用各模块 list 方法聚合计数，不新增表。
 */
@Service
public class StatsService {
    private final CustomerService customerService;
    private final ResourceService resourceService;
    private final PackageBuildService packageBuildService;
    private final AgentService agentService;

    public StatsService(CustomerService customerService, ResourceService resourceService,
                        PackageBuildService packageBuildService, AgentService agentService) {
        this.customerService = customerService;
        this.resourceService = resourceService;
        this.packageBuildService = packageBuildService;
        this.agentService = agentService;
    }

    public StatsOverview overview() {
        List<AgentTaskEntity> tasks = agentService.listOfflineTasks();
        Map<String, Long> statusCounts = tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.taskStatus().name(),
                        LinkedHashMap::new,
                        Collectors.counting()));
        return new StatsOverview(
                customerService.listCustomers().size(),
                resourceService.listResources().size(),
                packageBuildService.listPackages().size(),
                tasks.size(),
                statusCounts
        );
    }
}
