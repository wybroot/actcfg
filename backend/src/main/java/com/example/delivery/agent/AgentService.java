package com.example.delivery.agent;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    public List<AgentTaskEntity> listOfflineTasks() {
        return List.of(new AgentTaskEntity(1L, "TASK-001", 1L, "OFFLINE_DEPLOY", AgentTaskStatus.SUCCESS, LocalDateTime.now(), LocalDateTime.now(), "离线部署示例任务"));
    }

    public List<AgentExecutionLogEntity> listExecutionLogs(Long taskId) {
        return List.of(new AgentExecutionLogEntity(1L, taskId, "CHECK_ENV", "环境检测", AgentTaskStatus.SUCCESS, "INFO", "environment check passed", 0));
    }
}
