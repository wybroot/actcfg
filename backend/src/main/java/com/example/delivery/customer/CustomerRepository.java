package com.example.delivery.customer;

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
            resultSet.getString("variable_value"),
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
                SELECT id, environment_id, variable_key, variable_value, masked_value, is_sensitive
                FROM env_variable
                WHERE environment_id = ?
                ORDER BY id ASC
                """, variableMapper, environmentId);
    }

    public Optional<EnvVariableEntity> findVariableById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, environment_id, variable_key, variable_value, masked_value, is_sensitive
                    FROM env_variable WHERE id = ?
                    """, variableMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** 全部敏感变量（供密钥轮换扫描）。 */
    public List<EnvVariableEntity> findAllSensitiveVariables() {
        return jdbcTemplate.query("""
                SELECT id, environment_id, variable_key, variable_value, masked_value, is_sensitive
                FROM env_variable
                WHERE is_sensitive = 1
                ORDER BY id ASC
                """, variableMapper);
    }

    /** 仅更新存储值（密文），用于轮换重新加密，不改动 key/敏感标记。 */
    public void updateVariableValueRaw(Long id, String storedValue) {
        jdbcTemplate.update("UPDATE env_variable SET variable_value = ? WHERE id = ?", storedValue, id);
    }

    // ---- 客户增删改 ----

    public CustomerEntity insertCustomer(String code, String name, String shortName, String industry) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO customer (customer_code, customer_name, short_name, industry, status, created_at, deleted)
                    VALUES (?, ?, ?, ?, 'ENABLED', CURRENT_TIMESTAMP, 0)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code); ps.setString(2, name);
            ps.setString(3, shortName); ps.setString(4, industry);
            return ps;
        }, kh);
        return findActiveCustomerById(kh.getKey().longValue()).orElseThrow();
    }

    public CustomerEntity updateCustomer(Long id, String name, String shortName, String industry) {
        jdbcTemplate.update("""
                UPDATE customer SET customer_name=?, short_name=?, industry=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND deleted=0
                """, name, shortName, industry, id);
        return findActiveCustomerById(id).orElseThrow();
    }

    public void softDeleteCustomer(Long id) {
        jdbcTemplate.update("""
                UPDATE customer SET deleted=1, updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, id);
    }

    // ---- 环境增删改 ----

    public CustomerEnvironmentEntity insertEnvironment(Long customerId, String name, EnvironmentType type) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO customer_environment (customer_id, environment_name, environment_type, status, created_at)
                    VALUES (?, ?, ?, 'ENABLED', CURRENT_TIMESTAMP)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId); ps.setString(2, name); ps.setString(3, type.name());
            return ps;
        }, kh);
        return findEnvironmentById(kh.getKey().longValue()).orElseThrow();
    }

    // ---- 变量增删改 ----

    public EnvVariableEntity insertVariable(Long environmentId, String key, String value, boolean sensitive) {
        String masked = sensitive ? "******" : null;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO env_variable (environment_id, variable_key, variable_value, masked_value, is_sensitive)
                    VALUES (?, ?, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, environmentId); ps.setString(2, key);
            ps.setString(3, value); ps.setString(4, masked); ps.setBoolean(5, sensitive);
            return ps;
        }, kh);
        return findVariableById(kh.getKey().longValue()).orElseThrow();
    }

    public EnvVariableEntity updateVariable(Long id, String value, boolean sensitive) {
        String masked = sensitive ? "******" : null;
        jdbcTemplate.update("""
                UPDATE env_variable SET variable_value=?, masked_value=?, is_sensitive=? WHERE id=?
                """, value, masked, sensitive, id);
        return findVariableById(id).orElseThrow();
    }

    public void deleteVariable(Long id) {
        jdbcTemplate.update("DELETE FROM env_variable WHERE id=?", id);
    }
}
