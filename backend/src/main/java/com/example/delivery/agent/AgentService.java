package com.example.delivery.agent;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import com.example.delivery.packagebuild.PackageBuildEntity;
import com.example.delivery.packagebuild.PackageBuildService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {
    private final PackageBuildService packageBuildService;
    private final AgentRepository agentRepository;
    private final AtomicLong taskIdSequence = new AtomicLong(1);
    private final AtomicLong logIdSequence = new AtomicLong(1);
    private final AtomicLong reportIdSequence = new AtomicLong(1);
    private final AtomicLong retryRecordIdSequence = new AtomicLong(1);
    private final Map<Long, AgentTaskEntity> tasks = new ConcurrentHashMap<>();
    private final Map<Long, AgentExecutionLogEntity> logs = new ConcurrentHashMap<>();
    private final Map<Long, AgentExecutionReportEntity> reports = new ConcurrentHashMap<>();
    private final Map<Long, AgentRetryRecordEntity> retryRecords = new ConcurrentHashMap<>();

    @Autowired
    public AgentService(PackageBuildService packageBuildService, ObjectProvider<AgentRepository> agentRepositoryProvider) {
        this.packageBuildService = packageBuildService;
        this.agentRepository = agentRepositoryProvider.getIfAvailable();
        if (this.agentRepository == null) {
            seed();
        }
    }

    public AgentService(PackageBuildService packageBuildService) {
        this.packageBuildService = packageBuildService;
        this.agentRepository = null;
        seed();
    }

    public List<AgentTaskEntity> listOfflineTasks() {
        if (useJdbc()) {
            return agentRepository.findAllTasks();
        }
        return tasks.values().stream()
                .sorted(Comparator.comparing(AgentTaskEntity::startedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public AgentTaskEntity getTask(Long taskId) {
        if (useJdbc()) {
            return agentRepository.findTaskById(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Agent 任务不存在"));
        }
        AgentTaskEntity task = tasks.get(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Agent 任务不存在");
        }
        return task;
    }

    @Transactional
    public AgentTaskEntity createTask(CreateAgentTaskRequest request) {
        packageBuildService.getDownloadInfo(request.packageBuildId());
        Long id = taskIdSequence.getAndIncrement();
        String taskCode = "TASK-" + String.format("%04d", id);
        if (useJdbc()) {
            AgentTaskEntity task = agentRepository.insertTask(request, taskCode);
            agentRepository.insertLog(task.id(), "CREATE_TASK", "创建任务", AgentTaskStatus.PENDING, "INFO", "offline task created", 0);
            return task;
        }
        AgentTaskEntity task = new AgentTaskEntity(
                id,
                taskCode,
                request.packageBuildId(),
                request.taskType(),
                AgentTaskStatus.PENDING,
                null,
                null,
                "等待 Agent 拉取任务"
        );
        tasks.put(id, task);
        appendLog(id, "CREATE_TASK", "创建任务", AgentTaskStatus.PENDING, "INFO", "offline task created", 0);
        return task;
    }

    @Transactional
    public AgentTaskEntity cancelTask(Long taskId) {
        AgentTaskEntity current = getTask(taskId);
        if (isFinished(current.taskStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "已完成任务不能取消");
        }
        if (useJdbc()) {
            AgentTaskEntity canceled = agentRepository.cancelTask(current);
            agentRepository.insertLog(taskId, "CANCEL_TASK", "取消任务", AgentTaskStatus.CANCELED, "WARN", "offline task canceled", 0);
            return canceled;
        }
        AgentTaskEntity canceled = new AgentTaskEntity(
                current.id(),
                current.taskCode(),
                current.packageBuildId(),
                current.taskType(),
                AgentTaskStatus.CANCELED,
                current.startedAt(),
                LocalDateTime.now(),
                "任务已取消"
        );
        tasks.put(taskId, canceled);
        appendLog(taskId, "CANCEL_TASK", "取消任务", AgentTaskStatus.CANCELED, "WARN", "offline task canceled", 0);
        return canceled;
    }

    @Transactional
    public AgentTaskEntity reportStatus(Long taskId, ReportAgentStatusRequest request) {
        AgentTaskEntity current = getTask(taskId);
        if (isFinished(current.taskStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "已完成任务不能继续上报状态");
        }
        if (useJdbc()) {
            AgentTaskEntity updated = agentRepository.updateTaskStatus(current, request.taskStatus(), request.resultSummary());
            agentRepository.insertLog(
                    taskId,
                    request.stepCode(),
                    request.stepName(),
                    request.taskStatus(),
                    defaultLogLevel(request.logLevel()),
                    request.logContent(),
                    agentRepository.retryCount(taskId, request.stepCode())
            );
            if (isFinished(request.taskStatus())) {
                agentRepository.findOpenRetryRecord(taskId)
                        .ifPresent(record -> agentRepository.closeRetryRecord(record.id(), request.taskStatus()));
            }
            return updated;
        }
        LocalDateTime startedAt = current.startedAt();
        if (startedAt == null && request.taskStatus() == AgentTaskStatus.RUNNING) {
            startedAt = LocalDateTime.now();
        }
        LocalDateTime finishedAt = isFinished(request.taskStatus()) ? LocalDateTime.now() : null;
        AgentTaskEntity updated = new AgentTaskEntity(
                current.id(),
                current.taskCode(),
                current.packageBuildId(),
                current.taskType(),
                request.taskStatus(),
                startedAt,
                finishedAt,
                request.resultSummary()
        );
        tasks.put(taskId, updated);
        appendLog(
                taskId,
                request.stepCode(),
                request.stepName(),
                request.taskStatus(),
                defaultLogLevel(request.logLevel()),
                request.logContent(),
                retryCount(taskId, request.stepCode())
        );
        if (isFinished(request.taskStatus())) {
            closeOpenRetryRecord(taskId, request.taskStatus());
        }
        return updated;
    }

    @Transactional
    public AgentExecutionReportEntity importReport(Long taskId, ImportAgentReportRequest request) {
        AgentTaskEntity task = getTask(taskId);
        if (!isFinished(task.taskStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已完成任务才能导入执行报告");
        }
        Long id = reportIdSequence.getAndIncrement();
        String reportCode = "RPT-" + String.format("%04d", id);
        if (useJdbc()) {
            if (agentRepository.existsReportForTask(taskId)) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "该任务已导入执行报告");
            }
            PackageBuildEntity packageBuild = packageBuildService.getPackage(task.packageBuildId());
            return agentRepository.insertReport(reportCode, task, packageBuild.customerId(), packageBuild.environmentId(), request);
        }
        boolean exists = reports.values().stream().anyMatch(report -> report.taskId().equals(taskId));
        if (exists) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "该任务已导入执行报告");
        }
        PackageBuildEntity packageBuild = packageBuildService.getPackage(task.packageBuildId());
        AgentExecutionReportEntity report = new AgentExecutionReportEntity(
                id,
                reportCode,
                taskId,
                task.packageBuildId(),
                packageBuild.customerId(),
                packageBuild.environmentId(),
                request.executionHost(),
                task.taskStatus(),
                task.startedAt(),
                task.finishedAt(),
                request.failedStep(),
                request.failureReason(),
                request.healthCheckResult(),
                request.reportContent(),
                LocalDateTime.now()
        );
        reports.put(id, report);
        return report;
    }

    public AgentExecutionReportEntity getReport(Long taskId) {
        getTask(taskId);
        if (useJdbc()) {
            return agentRepository.findReportByTaskId(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "执行报告不存在"));
        }
        return reports.values().stream()
                .filter(report -> report.taskId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "执行报告不存在"));
    }

    public List<AgentExecutionReportEntity> listReports() {
        if (useJdbc()) {
            return agentRepository.findAllReports();
        }
        return reports.values().stream()
                .sorted(Comparator.comparing(AgentExecutionReportEntity::importedAt).reversed())
                .toList();
    }

    @Transactional
    public AgentTaskEntity retryTask(Long taskId) {
        AgentTaskEntity current = getTask(taskId);
        if (current.taskStatus() != AgentTaskStatus.FAILED) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有失败任务才能续跑");
        }
        String failedStep;
        String failureReason;
        if (useJdbc()) {
            AgentExecutionLogEntity latestLog = agentRepository.findLatestLog(taskId).orElse(null);
            failedStep = latestLog == null ? null : latestLog.stepCode();
            failureReason = latestLog == null ? null : latestLog.logContent();
            int retryNo = agentRepository.nextRetryNo(taskId);
            agentRepository.insertRetryRecord(taskId, retryNo, failedStep, failureReason);
            AgentTaskEntity updated = agentRepository.updateTaskStatus(current, AgentTaskStatus.RETRYING, "第 " + retryNo + " 次续跑");
            agentRepository.insertLog(taskId, failedStep == null ? "RETRY" : failedStep, "失败续跑", AgentTaskStatus.RETRYING, "WARN",
                    "retry from failed step: " + failedStep, retryNo);
            return updated;
        }
        AgentExecutionLogEntity latestLog = latestLog(taskId);
        failedStep = latestLog == null ? null : latestLog.stepCode();
        failureReason = latestLog == null ? null : latestLog.logContent();
        int retryNo = nextRetryNo(taskId);
        Long recordId = retryRecordIdSequence.getAndIncrement();
        retryRecords.put(recordId, new AgentRetryRecordEntity(recordId, taskId, retryNo, failedStep, failureReason, LocalDateTime.now(), null, null));
        AgentTaskEntity updated = new AgentTaskEntity(
                current.id(),
                current.taskCode(),
                current.packageBuildId(),
                current.taskType(),
                AgentTaskStatus.RETRYING,
                current.startedAt(),
                null,
                "第 " + retryNo + " 次续跑"
        );
        tasks.put(taskId, updated);
        appendLog(taskId, failedStep == null ? "RETRY" : failedStep, "失败续跑", AgentTaskStatus.RETRYING, "WARN",
                "retry from failed step: " + failedStep, retryNo);
        return updated;
    }

    public List<AgentRetryRecordView> listRetryRecords() {
        if (useJdbc()) {
            return agentRepository.findRetryRecordViews();
        }
        Map<Long, List<AgentRetryRecordEntity>> byTask = retryRecords.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(AgentRetryRecordEntity::taskId));
        return byTask.entrySet().stream()
                .map(entry -> {
                    Long taskId = entry.getKey();
                    List<AgentRetryRecordEntity> records = entry.getValue();
                    AgentRetryRecordEntity latest = records.stream()
                            .max(Comparator.comparing(AgentRetryRecordEntity::triggeredAt))
                            .orElseThrow();
                    AgentTaskEntity task = tasks.get(taskId);
                    return new AgentRetryRecordView(
                            taskId,
                            task == null ? null : task.taskCode(),
                            task == null ? null : task.packageBuildId(),
                            latest.failedStep(),
                            latest.failureReason(),
                            records.size(),
                            latest.triggeredAt(),
                            task == null ? null : task.taskStatus()
                    );
                })
                .sorted(Comparator.comparing(AgentRetryRecordView::lastRetryAt).reversed())
                .toList();
    }

    private AgentExecutionLogEntity latestLog(Long taskId) {
        return logs.values().stream()
                .filter(log -> log.taskId().equals(taskId))
                .max(Comparator.comparing(AgentExecutionLogEntity::id))
                .orElse(null);
    }

    private int nextRetryNo(Long taskId) {
        return (int) retryRecords.values().stream()
                .filter(record -> record.taskId().equals(taskId))
                .count() + 1;
    }

    private void closeOpenRetryRecord(Long taskId, AgentTaskStatus resultStatus) {
        retryRecords.values().stream()
                .filter(record -> record.taskId().equals(taskId) && record.finishedAt() == null)
                .max(Comparator.comparing(AgentRetryRecordEntity::retryNo))
                .ifPresent(record -> retryRecords.put(record.id(), new AgentRetryRecordEntity(
                        record.id(),
                        record.taskId(),
                        record.retryNo(),
                        record.failedStep(),
                        record.failureReason(),
                        record.triggeredAt(),
                        LocalDateTime.now(),
                        resultStatus
                )));
    }

    public List<AgentExecutionLogEntity> listExecutionLogs(Long taskId) {
        getTask(taskId);
        if (useJdbc()) {
            return agentRepository.findLogsByTaskId(taskId);
        }
        return logs.values().stream()
                .filter(log -> log.taskId().equals(taskId))
                .sorted(Comparator.comparing(AgentExecutionLogEntity::id))
                .toList();
    }

    private void seed() {
        AgentTaskEntity task = new AgentTaskEntity(
                taskIdSequence.getAndIncrement(),
                "TASK-0001",
                1L,
                "OFFLINE_DEPLOY",
                AgentTaskStatus.SUCCESS,
                LocalDateTime.now().minusMinutes(3),
                LocalDateTime.now(),
                "离线部署示例任务"
        );
        tasks.put(task.id(), task);
        appendLog(task.id(), "CHECK_ENV", "环境检测", AgentTaskStatus.SUCCESS, "INFO", "environment check passed", 0);
        appendLog(task.id(), "DEPLOY", "执行部署", AgentTaskStatus.SUCCESS, "INFO", "deploy finished", 0);
        appendLog(task.id(), "HEALTH_CHECK", "健康检查", AgentTaskStatus.SUCCESS, "INFO", "health check passed", 0);
    }

    private void appendLog(
            Long taskId,
            String stepCode,
            String stepName,
            AgentTaskStatus stepStatus,
            String logLevel,
            String logContent,
            int retryCount
    ) {
        Long id = logIdSequence.getAndIncrement();
        logs.put(id, new AgentExecutionLogEntity(
                id,
                taskId,
                stepCode,
                stepName,
                stepStatus,
                logLevel,
                logContent,
                retryCount
        ));
    }

    private int retryCount(Long taskId, String stepCode) {
        return (int) logs.values().stream()
                .filter(log -> log.taskId().equals(taskId) && log.stepCode().equals(stepCode))
                .count();
    }

    private String defaultLogLevel(String logLevel) {
        return logLevel == null || logLevel.isBlank() ? "INFO" : logLevel;
    }

    private boolean isFinished(AgentTaskStatus status) {
        return status == AgentTaskStatus.SUCCESS || status == AgentTaskStatus.FAILED || status == AgentTaskStatus.CANCELED;
    }

    private boolean useJdbc() {
        return agentRepository != null;
    }
}
