package com.example.delivery.agent;

import com.example.delivery.common.jdbc.JdbcMapperSupport;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class AgentRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentTaskEntity> taskMapper = (resultSet, rowNum) -> new AgentTaskEntity(
            resultSet.getLong("id"),
            resultSet.getString("task_code"),
            resultSet.getLong("package_build_id"),
            resultSet.getString("task_type"),
            AgentTaskStatus.valueOf(resultSet.getString("task_status")),
            JdbcMapperSupport.nullableDateTime(resultSet, "started_at"),
            JdbcMapperSupport.nullableDateTime(resultSet, "finished_at"),
            resultSet.getString("result_summary")
    );

    private final RowMapper<AgentExecutionLogEntity> logMapper = (resultSet, rowNum) -> new AgentExecutionLogEntity(
            resultSet.getLong("id"),
            resultSet.getLong("task_id"),
            resultSet.getString("step_code"),
            resultSet.getString("step_name"),
            AgentTaskStatus.valueOf(resultSet.getString("step_status")),
            resultSet.getString("log_level"),
            resultSet.getString("log_content"),
            resultSet.getInt("retry_count")
    );

    private final RowMapper<AgentExecutionReportEntity> reportMapper = (resultSet, rowNum) -> new AgentExecutionReportEntity(
            resultSet.getLong("id"),
            resultSet.getString("report_code"),
            resultSet.getLong("task_id"),
            resultSet.getLong("package_build_id"),
            resultSet.getLong("customer_id"),
            resultSet.getLong("environment_id"),
            resultSet.getString("execution_host"),
            AgentTaskStatus.valueOf(resultSet.getString("execution_status")),
            JdbcMapperSupport.nullableDateTime(resultSet, "started_at"),
            JdbcMapperSupport.nullableDateTime(resultSet, "finished_at"),
            resultSet.getString("failed_step"),
            resultSet.getString("failure_reason"),
            resultSet.getString("health_check_result"),
            resultSet.getString("report_content"),
            JdbcMapperSupport.nullableDateTime(resultSet, "imported_at")
    );

    private final RowMapper<AgentRetryRecordEntity> retryRecordMapper = (resultSet, rowNum) -> new AgentRetryRecordEntity(
            resultSet.getLong("id"),
            resultSet.getLong("task_id"),
            resultSet.getInt("retry_no"),
            resultSet.getString("failed_step"),
            resultSet.getString("failure_reason"),
            JdbcMapperSupport.nullableDateTime(resultSet, "triggered_at"),
            JdbcMapperSupport.nullableDateTime(resultSet, "finished_at"),
            resultSet.getString("result_status") == null ? null : AgentTaskStatus.valueOf(resultSet.getString("result_status"))
    );

    private final RowMapper<AgentRetryRecordView> retryRecordViewMapper = (resultSet, rowNum) -> new AgentRetryRecordView(
            resultSet.getLong("task_id"),
            resultSet.getString("task_code"),
            resultSet.getLong("package_build_id"),
            resultSet.getString("failed_step"),
            resultSet.getString("failure_reason"),
            resultSet.getInt("retry_count"),
            JdbcMapperSupport.nullableDateTime(resultSet, "last_retry_at"),
            resultSet.getString("final_status") == null ? null : AgentTaskStatus.valueOf(resultSet.getString("final_status"))
    );

    public AgentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AgentTaskEntity> findAllTasks() {
        return jdbcTemplate.query("""
                SELECT id, task_code, package_build_id, task_type, task_status, started_at, finished_at, result_summary
                FROM agent_task
                ORDER BY COALESCE(started_at, '1970-01-01') DESC, id DESC
                """, taskMapper);
    }

    public Optional<AgentTaskEntity> findTaskById(Long taskId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, task_code, package_build_id, task_type, task_status, started_at, finished_at, result_summary
                    FROM agent_task
                    WHERE id = ?
                    """, taskMapper, taskId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public AgentTaskEntity insertTask(CreateAgentTaskRequest request, String taskCode) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO agent_task (task_code, package_build_id, task_type, task_status, result_summary)
                    VALUES (?, ?, ?, 'PENDING', '等待 Agent 拉取任务')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, taskCode);
            statement.setLong(2, request.packageBuildId());
            statement.setString(3, request.taskType());
            return statement;
        }, keyHolder);
        return findTaskById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public AgentTaskEntity updateTaskStatus(AgentTaskEntity current, AgentTaskStatus status, String resultSummary) {
        jdbcTemplate.update("""
                UPDATE agent_task
                SET task_status = ?,
                    started_at = CASE WHEN started_at IS NULL AND ? = 'RUNNING' THEN CURRENT_TIMESTAMP ELSE started_at END,
                    finished_at = CASE WHEN ? IN ('SUCCESS', 'FAILED', 'CANCELED') THEN CURRENT_TIMESTAMP ELSE NULL END,
                    result_summary = ?
                WHERE id = ?
                """, status.name(), status.name(), status.name(), resultSummary, current.id());
        return findTaskById(current.id()).orElseThrow();
    }

    public AgentTaskEntity cancelTask(AgentTaskEntity current) {
        jdbcTemplate.update("""
                UPDATE agent_task
                SET task_status = 'CANCELED', finished_at = CURRENT_TIMESTAMP, result_summary = '任务已取消'
                WHERE id = ?
                """, current.id());
        return findTaskById(current.id()).orElseThrow();
    }

    public void insertLog(Long taskId, String stepCode, String stepName, AgentTaskStatus status, String logLevel, String logContent, int retryCount) {
        jdbcTemplate.update("""
                INSERT INTO agent_execution_log (task_id, step_code, step_name, step_status, log_level, log_content, retry_count, started_at, finished_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, taskId, stepCode, stepName, status.name(), logLevel, logContent, retryCount);
    }

    public List<AgentExecutionLogEntity> findLogsByTaskId(Long taskId) {
        return jdbcTemplate.query("""
                SELECT id, task_id, step_code, step_name, step_status, log_level, log_content, retry_count
                FROM agent_execution_log
                WHERE task_id = ?
                ORDER BY id ASC
                """, logMapper, taskId);
    }

    public int retryCount(Long taskId, String stepCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_execution_log WHERE task_id = ? AND step_code = ?",
                Integer.class,
                taskId,
                stepCode
        );
        return count == null ? 0 : count;
    }

    public Optional<AgentExecutionLogEntity> findLatestLog(Long taskId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, task_id, step_code, step_name, step_status, log_level, log_content, retry_count
                    FROM agent_execution_log
                    WHERE task_id = ?
                    ORDER BY id DESC
                    LIMIT 1
                    """, logMapper, taskId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsReportForTask(Long taskId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_execution_report WHERE task_id = ?",
                Integer.class,
                taskId
        );
        return count != null && count > 0;
    }

    public AgentExecutionReportEntity insertReport(
            String reportCode,
            AgentTaskEntity task,
            Long customerId,
            Long environmentId,
            ImportAgentReportRequest request
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO agent_execution_report (
                        report_code, task_id, package_build_id, customer_id, environment_id,
                        execution_host, execution_status, started_at, finished_at,
                        failed_step, failure_reason, health_check_result, report_content, imported_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, reportCode);
            statement.setLong(2, task.id());
            statement.setLong(3, task.packageBuildId());
            statement.setLong(4, customerId);
            statement.setLong(5, environmentId);
            statement.setString(6, request.executionHost());
            statement.setString(7, task.taskStatus().name());
            JdbcMapperSupport.setNullableTimestamp(statement, 8, task.startedAt());
            JdbcMapperSupport.setNullableTimestamp(statement, 9, task.finishedAt());
            statement.setString(10, request.failedStep());
            statement.setString(11, request.failureReason());
            statement.setString(12, request.healthCheckResult());
            statement.setString(13, request.reportContent());
            return statement;
        }, keyHolder);
        return findReportById(keyHolder.getKey().longValue()).orElseThrow();
    }

    private Optional<AgentExecutionReportEntity> findReportById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, report_code, task_id, package_build_id, customer_id, environment_id,
                           execution_host, execution_status, started_at, finished_at,
                           failed_step, failure_reason, health_check_result, report_content, imported_at
                    FROM agent_execution_report
                    WHERE id = ?
                    """, reportMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<AgentExecutionReportEntity> findAllReports() {
        return jdbcTemplate.query("""
                SELECT id, report_code, task_id, package_build_id, customer_id, environment_id,
                       execution_host, execution_status, started_at, finished_at,
                       failed_step, failure_reason, health_check_result, report_content, imported_at
                FROM agent_execution_report
                ORDER BY imported_at DESC, id DESC
                """, reportMapper);
    }

    public Optional<AgentExecutionReportEntity> findReportByTaskId(Long taskId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, report_code, task_id, package_build_id, customer_id, environment_id,
                           execution_host, execution_status, started_at, finished_at,
                           failed_step, failure_reason, health_check_result, report_content, imported_at
                    FROM agent_execution_report
                    WHERE task_id = ?
                    """, reportMapper, taskId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public int nextRetryNo(Long taskId) {
        Integer maxRetryNo = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(retry_no), 0) FROM agent_retry_record WHERE task_id = ?",
                Integer.class,
                taskId
        );
        return (maxRetryNo == null ? 0 : maxRetryNo) + 1;
    }

    public AgentRetryRecordEntity insertRetryRecord(Long taskId, int retryNo, String failedStep, String failureReason) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO agent_retry_record (task_id, retry_no, failed_step, failure_reason, triggered_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, taskId);
            statement.setInt(2, retryNo);
            statement.setString(3, failedStep);
            statement.setString(4, failureReason);
            return statement;
        }, keyHolder);
        return findRetryRecordById(keyHolder.getKey().longValue()).orElseThrow();
    }

    private Optional<AgentRetryRecordEntity> findRetryRecordById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, task_id, retry_no, failed_step, failure_reason, triggered_at, finished_at, result_status
                    FROM agent_retry_record
                    WHERE id = ?
                    """, retryRecordMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<AgentRetryRecordEntity> findOpenRetryRecord(Long taskId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, task_id, retry_no, failed_step, failure_reason, triggered_at, finished_at, result_status
                    FROM agent_retry_record
                    WHERE task_id = ? AND finished_at IS NULL
                    ORDER BY retry_no DESC
                    LIMIT 1
                    """, retryRecordMapper, taskId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void closeRetryRecord(Long id, AgentTaskStatus resultStatus) {
        jdbcTemplate.update("""
                UPDATE agent_retry_record
                SET finished_at = CURRENT_TIMESTAMP, result_status = ?
                WHERE id = ?
                """, resultStatus.name(), id);
    }

    public List<AgentRetryRecordView> findRetryRecordViews() {
        return jdbcTemplate.query("""
                SELECT
                    t.id AS task_id,
                    t.task_code AS task_code,
                    t.package_build_id AS package_build_id,
                    r.failed_step AS failed_step,
                    r.failure_reason AS failure_reason,
                    COUNT(r.id) AS retry_count,
                    MAX(r.triggered_at) AS last_retry_at,
                    t.task_status AS final_status
                FROM agent_retry_record r
                JOIN agent_task t ON t.id = r.task_id
                GROUP BY t.id, t.task_code, t.package_build_id, r.failed_step, r.failure_reason, t.task_status
                ORDER BY MAX(r.triggered_at) DESC
                """, retryRecordViewMapper);
    }
}
