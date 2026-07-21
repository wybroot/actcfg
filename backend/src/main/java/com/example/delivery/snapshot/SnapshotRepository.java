package com.example.delivery.snapshot;

import com.example.delivery.common.jdbc.JdbcMapperSupport;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class SnapshotRepository {
    private final JdbcTemplate jdbc;

    public SnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private SnapshotEntity mapSnapshot(ResultSet rs, int i) throws SQLException {
        return new SnapshotEntity(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getLong("environment_id"),
                rs.getLong("source_plan_version_id"),
                rs.getString("plan_name"),
                rs.getString("version_label"),
                rs.getString("status"),
                JdbcMapperSupport.nullableDateTime(rs, "created_at")
        );
    }

    private SnapshotComponentEntity mapComponent(ResultSet rs, int i) throws SQLException {
        return new SnapshotComponentEntity(
                rs.getLong("id"),
                rs.getLong("snapshot_id"),
                rs.getString("component_name"),
                rs.getString("component_type"),
                rs.getObject("resource_version_id", Long.class),
                rs.getInt("deploy_order"),
                rs.getString("config_template"),
                rs.getString("health_check")
        );
    }

    public Optional<SnapshotEntity> findByEnvironmentId(Long environmentId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT * FROM customer_deploy_snapshot
                    WHERE environment_id = ? AND status = 'ACTIVE'
                    ORDER BY id DESC LIMIT 1
                    """, this::mapSnapshot, environmentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<SnapshotEntity> findById(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM customer_deploy_snapshot WHERE id = ?", this::mapSnapshot, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<SnapshotComponentEntity> findComponents(Long snapshotId) {
        return jdbc.query("""
                SELECT * FROM customer_deploy_snapshot_component
                WHERE snapshot_id = ? ORDER BY deploy_order ASC, id ASC
                """, this::mapComponent, snapshotId);
    }

    /** 停用某环境已有快照（重新绑定时调用）。 */
    public void deactivateByEnvironment(Long environmentId) {
        jdbc.update("UPDATE customer_deploy_snapshot SET status='REPLACED' WHERE environment_id=? AND status='ACTIVE'",
                environmentId);
    }

    public SnapshotEntity insertSnapshot(SnapshotEntity s) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO customer_deploy_snapshot
                    (customer_id, environment_id, source_plan_version_id, plan_name, version_label, status, created_at)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, s.customerId());
            ps.setLong(2, s.environmentId());
            ps.setLong(3, s.sourcePlanVersionId());
            ps.setString(4, s.planName());
            ps.setString(5, s.versionLabel());
            return ps;
        }, kh);
        return findById(kh.getKey().longValue()).orElseThrow();
    }

    public void insertComponent(Long snapshotId, SnapshotComponentEntity c) {
        jdbc.update("""
                INSERT INTO customer_deploy_snapshot_component
                (snapshot_id, component_name, component_type, resource_version_id, deploy_order, config_template, health_check)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, snapshotId, c.componentName(), c.componentType(), c.resourceVersionId(),
                c.deployOrder(), c.configTemplate(), c.healthCheck());
    }

    public void updateComponentConfig(Long componentId, String configTemplate) {
        jdbc.update("UPDATE customer_deploy_snapshot_component SET config_template=? WHERE id=?",
                configTemplate, componentId);
    }

    public Optional<SnapshotComponentEntity> findComponentById(Long componentId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM customer_deploy_snapshot_component WHERE id = ?", this::mapComponent, componentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
