package com.example.delivery.agent;

import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents/offline")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<AgentTaskEntity>> listOfflineTasks() {
        return ApiResponse.ok(agentService.listOfflineTasks());
    }

    @PostMapping("/tasks")
    public ApiResponse<AgentTaskEntity> createTask(@Valid @RequestBody CreateAgentTaskRequest request) {
        return ApiResponse.ok(agentService.createTask(request));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<AgentTaskEntity> getTask(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.getTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ApiResponse<AgentTaskEntity> cancelTask(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.cancelTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/status")
    public ApiResponse<AgentTaskEntity> reportStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody ReportAgentStatusRequest request
    ) {
        return ApiResponse.ok(agentService.reportStatus(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<List<AgentExecutionLogEntity>> listExecutionLogs(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.listExecutionLogs(taskId));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<AgentTaskEntity> retryTask(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.retryTask(taskId));
    }

    @GetMapping("/retry-records")
    public ApiResponse<List<AgentRetryRecordView>> listRetryRecords() {
        return ApiResponse.ok(agentService.listRetryRecords());
    }

    @PostMapping("/reports/import")
    public ApiResponse<AgentExecutionReportEntity> importReport(@Valid @RequestBody ImportAgentReportRequest request) {
        return ApiResponse.ok(agentService.importReport(request.taskId(), request));
    }

    @GetMapping("/reports")
    public ApiResponse<List<AgentExecutionReportEntity>> listReports() {
        return ApiResponse.ok(agentService.listReports());
    }

    @GetMapping("/tasks/{taskId}/report")
    public ApiResponse<AgentExecutionReportEntity> getReport(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.getReport(taskId));
    }
}
