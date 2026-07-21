package com.example.delivery.audit;

import com.example.delivery.common.jdbc.JdbcMapperSupport;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OperationLogEntity> findOperationLogs() {
        return jdbc.query("""
                SELECT id, operator_name, module, action, result, created_at
                FROM audit_operation_log
                ORDER BY created_at DESC, id DESC
                LIMIT 200
                """, (rs, i) -> new OperationLogEntity(
                rs.getLong("id"), rs.getString("operator_name"), rs.getString("module"),
                rs.getString("action"), rs.getString("result"),
                JdbcMapperSupport.nullableDateTime(rs, "created_at")));
    }

    public List<DownloadLogEntity> findDownloadLogs() {
        return jdbc.query("""
                SELECT id, downloader_name, target_type, target_name, ip_address, created_at
                FROM audit_download_log
                ORDER BY created_at DESC, id DESC
                LIMIT 200
                """, (rs, i) -> new DownloadLogEntity(
                rs.getLong("id"), rs.getString("downloader_name"), rs.getString("target_type"),
                rs.getString("target_name"), rs.getString("ip_address"),
                JdbcMapperSupport.nullableDateTime(rs, "created_at")));
    }

    public void insertOperationLog(String operatorName, String module, String action,
                                   String result, String ip, String paramSummary) {
        jdbc.update("""
                INSERT INTO audit_operation_log (operator_name, module, action, result, ip_address, parameter_summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, operatorName, module, action, result, ip, paramSummary);
    }

    public void insertDownloadLog(String downloaderName, String targetType, String targetName,
                                  Long customerId, Long environmentId, Long fileSize, String ip) {
        jdbc.update("""
                INSERT INTO audit_download_log (downloader_name, target_type, target_name, customer_id, environment_id, file_size, ip_address, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, downloaderName, targetType, targetName, customerId, environmentId, fileSize, ip);
    }
}
