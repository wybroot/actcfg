package com.example.delivery.deploy;

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
public class DeployPlanRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DeployPlanEntity> planMapper = (resultSet, rowNum) -> new DeployPlanEntity(
            resultSet.getLong("id"),
            resultSet.getString("plan_code"),
            resultSet.getString("plan_name"),
            resultSet.getObject("current_version_id", Long.class),
            resultSet.getString("status"),
            JdbcMapperSupport.nullableDateTime(resultSet, "created_at")
    );

    private final RowMapper<DeployPlanVersionEntity> versionMapper = (resultSet, rowNum) -> {
        DeployPlanVersionStatus status = DeployPlanVersionStatus.valueOf(resultSet.getString("status"));
        return new DeployPlanVersionEntity(
                resultSet.getLong("id"),
                resultSet.getLong("plan_id"),
                resultSet.getString("version"),
                status,
                status == DeployPlanVersionStatus.DRAFT,
                JdbcMapperSupport.nullableDateTime(resultSet, "created_at")
        );
    };

    private final RowMapper<DeployComponentEntity> componentMapper = (resultSet, rowNum) -> new DeployComponentEntity(
            resultSet.getLong("id"),
            resultSet.getLong("plan_version_id"),
            resultSet.getString("component_name"),
            resultSet.getString("component_type"),
            resultSet.getLong("resource_version_id"),
            resultSet.getInt("deploy_order"),
            resultSet.getString("config_template"),
            resultSet.getString("health_check")
    );

    public DeployPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DeployPlanEntity> findAllActivePlans() {
        return jdbcTemplate.query("""
                SELECT id, plan_code, plan_name, current_version_id, status, created_at
                FROM deploy_plan
                WHERE deleted = 0
                ORDER BY created_at DESC, id DESC
                """, planMapper);
    }

    public Optional<DeployPlanEntity> findActivePlanById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, plan_code, plan_name, current_version_id, status, created_at
                    FROM deploy_plan
                    WHERE id = ? AND deleted = 0
                    """, planMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsPlanCode(String planCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM deploy_plan WHERE plan_code = ? AND deleted = 0",
                Integer.class,
                planCode
        );
        return count != null && count > 0;
    }

    public DeployPlanEntity insertPlan(CreateDeployPlanRequest request, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deploy_plan (plan_code, plan_name, description, status, created_at, updated_at, deleted)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.planCode());
            statement.setString(2, request.planName());
            statement.setString(3, request.description());
            statement.setString(4, status);
            return statement;
        }, keyHolder);
        return findActivePlanById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public List<DeployPlanVersionEntity> findVersionsByPlanId(Long planId) {
        return jdbcTemplate.query("""
                SELECT id, plan_id, version, status, created_at
                FROM deploy_plan_version
                WHERE plan_id = ?
                ORDER BY created_at DESC, id DESC
                """, versionMapper, planId);
    }

    public Optional<DeployPlanVersionEntity> findVersionById(Long versionId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, plan_id, version, status, created_at
                    FROM deploy_plan_version
                    WHERE id = ?
                    """, versionMapper, versionId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsVersion(Long planId, String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM deploy_plan_version WHERE plan_id = ? AND version = ?",
                Integer.class,
                planId,
                version
        );
        return count != null && count > 0;
    }

    public DeployPlanVersionEntity insertVersion(Long planId, CreateDeployPlanVersionRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deploy_plan_version (plan_id, version, status, created_at)
                    VALUES (?, ?, 'DRAFT', CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, planId);
            statement.setString(2, request.version());
            return statement;
        }, keyHolder);
        return findVersionById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public DeployPlanVersionEntity publishVersion(DeployPlanVersionEntity current) {
        jdbcTemplate.update("""
                UPDATE deploy_plan_version
                SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, current.id());
        jdbcTemplate.update("UPDATE deploy_plan SET current_version_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                current.id(), current.planId());
        return findVersionById(current.id()).orElseThrow();
    }

    public List<DeployComponentEntity> findComponentsByVersionId(Long versionId) {
        return jdbcTemplate.query("""
                SELECT id, plan_version_id, component_name, component_type, resource_version_id, deploy_order, config_template, health_check
                FROM deploy_component
                WHERE plan_version_id = ?
                ORDER BY deploy_order ASC, id ASC
                """, componentMapper, versionId);
    }

    public DeployComponentEntity insertComponent(Long versionId, CreateDeployComponentRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO deploy_component (plan_version_id, component_name, component_type, resource_version_id, deploy_order, config_template, health_check)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, versionId);
            statement.setString(2, request.componentName());
            statement.setString(3, request.componentType());
            statement.setLong(4, request.resourceVersionId());
            statement.setInt(5, request.deployOrder());
            statement.setString(6, request.configTemplate());
            statement.setString(7, request.healthCheck());
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        return findComponentsByVersionId(versionId).stream()
                .filter(component -> component.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
