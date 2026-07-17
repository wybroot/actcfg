package com.example.delivery.agent;

import com.example.delivery.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<List<AgentExecutionLogEntity>> listExecutionLogs(@PathVariable Long taskId) {
        return ApiResponse.ok(agentService.listExecutionLogs(taskId));
    }
}
