package com.example.delivery.agent;

import com.example.delivery.audit.AuditLog;
import com.example.delivery.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS','IMPL_ENGINEER')")
    @AuditLog(module = "AGENT", action = "CREATE_TASK")
    public ApiResponse<AgentTaskEntity> createTask(@Valid @RequestBody CreateAgentTaskRequest request) {
        return ApiResponse.ok(agentService.createTask(request));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<AgentTaskEntity> getTask(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.getTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS','IMPL_ENGINEER')")
    @AuditLog(module = "AGENT", action = "CANCEL_TASK")
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

    /** 实时日志流（SSE）：任务上报状态时推送，供前端实时观看部署进度。 */
    @GetMapping(value = "/tasks/{taskId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable Long taskId) {
        return agentService.streamLogs(taskId);
    }

    @PostMapping("/tasks/{taskId}/retry")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS','IMPL_ENGINEER')")
    @AuditLog(module = "AGENT", action = "RETRY_TASK")
    public ApiResponse<AgentTaskEntity> retryTask(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.retryTask(taskId));
    }

    @GetMapping("/retry-records")
    public ApiResponse<List<AgentRetryRecordView>> listRetryRecords() {
        return ApiResponse.ok(agentService.listRetryRecords());
    }

    @PostMapping("/reports/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPS','IMPL_ENGINEER')")
    @AuditLog(module = "AGENT", action = "IMPORT_REPORT")
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

    // ==================== 在线 Agent ====================

    @GetMapping("/instances")
    public ApiResponse<List<AgentInstanceEntity>> listInstances() {
        return ApiResponse.ok(agentService.listInstances());
    }

    @PostMapping("/instances/register")
    @AuditLog(module = "AGENT", action = "REGISTER_INSTANCE")
    public ApiResponse<AgentInstanceEntity> register(
            @Valid @RequestBody RegisterAgentRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.ok(agentService.registerInstance(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/instances/{agentCode}/heartbeat")
    public ApiResponse<AgentInstanceEntity> heartbeat(@PathVariable String agentCode) {
        return ApiResponse.ok(agentService.heartbeat(agentCode));
    }

    @PostMapping("/instances/{agentCode}/claim")
    @AuditLog(module = "AGENT", action = "CLAIM_TASK")
    public ApiResponse<AgentTaskEntity> claimNextTask(@PathVariable String agentCode) {
        return ApiResponse.ok(agentService.claimNextTask(agentCode));
    }
}
