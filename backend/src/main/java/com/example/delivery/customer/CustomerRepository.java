package com.example.delivery.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class CustomerRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CustomerEntity> customerMapper = (resultSet, rowNum) -> new CustomerEntity(
            resultSet.getLong("id"),
            resultSet.getString("customer_code"),
            resultSet.getString("customer_name"),
            resultSet.getString("short_name"),
            resultSet.getString("industry"),
            resultSet.getString("status")
    );

    private final RowMapper<CustomerEnvironmentEntity> environmentMapper = (resultSet, rowNum) -> new CustomerEnvironmentEntity(
            resultSet.getLong("id"),
            resultSet.getLong("customer_id"),
            resultSet.getString("environment_name"),
            EnvironmentType.valueOf(resultSet.getString("environment_type")),
            resultSet.getObject("deploy_plan_version_id", Long.class),
            resultSet.getString("status")
    );

    private final RowMapper<EnvVariableEntity> variableMapper = (resultSet, rowNum) -> new EnvVariableEntity(
            resultSet.getLong("id"),
            resultSet.getLong("environment_id"),
            resultSet.getString("variable_key"),
            resultSet.getString("masked_value"),
            resultSet.getBoolean("is_sensitive")
    );

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CustomerEntity> findAllActiveCustomers() {
        return jdbcTemplate.query("""
                SELECT id, customer_code, customer_name, short_name, industry, status
                FROM customer
                WHERE deleted = 0
                ORDER BY created_at DESC, id DESC
                """, customerMapper);
    }

    public Optional<CustomerEntity> findActiveCustomerById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, customer_code, customer_name, short_name, industry, status
                    FROM customer
                    WHERE id = ? AND deleted = 0
                    """, customerMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<CustomerEnvironmentEntity> findEnvironmentsByCustomerId(Long customerId) {
        return jdbcTemplate.query("""
                SELECT id, customer_id, environment_name, environment_type, deploy_plan_version_id, status
                FROM customer_environment
                WHERE customer_id = ?
                ORDER BY id ASC
                """, environmentMapper, customerId);
    }

    public Optional<CustomerEnvironmentEntity> findEnvironmentById(Long environmentId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, customer_id, environment_name, environment_type, deploy_plan_version_id, status
                    FROM customer_environment
                    WHERE id = ?
                    """, environmentMapper, environmentId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public CustomerEnvironmentEntity updateEnvironmentDeployPlanVersion(Long environmentId, Long deployPlanVersionId) {
        jdbcTemplate.update("""
                UPDATE customer_environment
                SET deploy_plan_version_id = ?
                WHERE id = ?
                """, deployPlanVersionId, environmentId);
        return findEnvironmentById(environmentId).orElseThrow();
    }

    public List<EnvVariableEntity> findVariablesByEnvironmentId(Long environmentId) {
        return jdbcTemplate.query("""
                SELECT id, environment_id, variable_key, masked_value, is_sensitive
                FROM env_variable
                WHERE environment_id = ?
                ORDER BY id ASC
                """, variableMapper, environmentId);
    }
}
