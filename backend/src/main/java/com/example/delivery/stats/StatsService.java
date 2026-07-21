package com.example.delivery.stats;

import com.example.delivery.agent.AgentExecutionReportEntity;
import com.example.delivery.agent.AgentService;
import com.example.delivery.agent.AgentTaskEntity;
import com.example.delivery.agent.AgentTaskStatus;
import com.example.delivery.customer.CustomerService;
import com.example.delivery.packagebuild.PackageBuildService;
import com.example.delivery.repository.ResourceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * 部署成功率与失败归因：
     * - 成功率基于"已结束"任务（SUCCESS/FAILED/CANCELED/SKIPPED），排除仍在进行的 PENDING/RUNNING/RETRYING。
     * - 失败归因取自导入的执行报告 failedStep / failureReason，按出现次数降序取 Top 5。
     */
    public DeployStats deployStats() {
        List<AgentTaskEntity> tasks = agentService.listOfflineTasks();
        long success = countStatus(tasks, AgentTaskStatus.SUCCESS);
        long failed = countStatus(tasks, AgentTaskStatus.FAILED);
        long canceled = countStatus(tasks, AgentTaskStatus.CANCELED);
        long skipped = countStatus(tasks, AgentTaskStatus.SKIPPED);
        long finished = success + failed + canceled + skipped;
        double rate = finished == 0 ? 0.0
                : BigDecimal.valueOf(success * 100.0 / finished)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();

        List<AgentExecutionReportEntity> reports = agentService.listReports();
        List<DeployStats.Attribution> topSteps = topAttribution(reports, AgentExecutionReportEntity::failedStep);
        List<DeployStats.Attribution> topReasons = topAttribution(reports, AgentExecutionReportEntity::failureReason);

        return new DeployStats(finished, success, failed, canceled, rate, topSteps, topReasons);
    }

    private long countStatus(List<AgentTaskEntity> tasks, AgentTaskStatus status) {
        return tasks.stream().filter(t -> t.taskStatus() == status).count();
    }

    private List<DeployStats.Attribution> topAttribution(
            List<AgentExecutionReportEntity> reports,
            java.util.function.Function<AgentExecutionReportEntity, String> extractor) {
        return reports.stream()
                .filter(r -> r.executionStatus() == AgentTaskStatus.FAILED)
                .map(extractor)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(e -> new DeployStats.Attribution(e.getKey(), e.getValue()))
                .toList();
    }
}
