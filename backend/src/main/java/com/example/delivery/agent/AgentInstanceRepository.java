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
public class AgentInstanceRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentInstanceEntity> mapper = (rs, rowNum) -> new AgentInstanceEntity(
            rs.getLong("id"),
            rs.getString("agent_code"),
            rs.getString("hostname"),
            rs.getString("ip_address"),
            rs.getObject("customer_id", Long.class),
            rs.getObject("environment_id", Long.class),
            rs.getString("instance_status"),
            JdbcMapperSupport.nullableDateTime(rs, "last_heartbeat_at"),
            JdbcMapperSupport.nullableDateTime(rs, "registered_at")
    );

    public AgentInstanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AgentInstanceEntity> findAll() {
        return jdbcTemplate.query("""
                SELECT id, agent_code, hostname, ip_address, customer_id, environment_id,
                       instance_status, last_heartbeat_at, registered_at
                FROM agent_instance
                ORDER BY registered_at DESC, id DESC
                """, mapper);
    }

    public Optional<AgentInstanceEntity> findByCode(String agentCode) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, agent_code, hostname, ip_address, customer_id, environment_id,
                           instance_status, last_heartbeat_at, registered_at
                    FROM agent_instance WHERE agent_code = ?
                    """, mapper, agentCode));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public AgentInstanceEntity insert(RegisterAgentRequest request, String ip) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO agent_instance (agent_code, hostname, ip_address, customer_id, environment_id,
                            instance_status, last_heartbeat_at, registered_at)
                    VALUES (?, ?, ?, ?, ?, 'ONLINE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.agentCode());
            ps.setString(2, request.hostname());
            ps.setString(3, ip);
            if (request.customerId() != null) ps.setLong(4, request.customerId()); else ps.setNull(4, java.sql.Types.BIGINT);
            if (request.environmentId() != null) ps.setLong(5, request.environmentId()); else ps.setNull(5, java.sql.Types.BIGINT);
            return ps;
        }, keyHolder);
        return findByCode(request.agentCode()).orElseThrow();
    }

    public AgentInstanceEntity reRegister(RegisterAgentRequest request, String ip) {
        jdbcTemplate.update("""
                UPDATE agent_instance
                SET hostname = ?, ip_address = ?, customer_id = ?, environment_id = ?,
                    instance_status = 'ONLINE', last_heartbeat_at = CURRENT_TIMESTAMP
                WHERE agent_code = ?
                """, request.hostname(), ip, request.customerId(), request.environmentId(), request.agentCode());
        return findByCode(request.agentCode()).orElseThrow();
    }

    public void heartbeat(String agentCode) {
        jdbcTemplate.update("""
                UPDATE agent_instance
                SET last_heartbeat_at = CURRENT_TIMESTAMP, instance_status = 'ONLINE'
                WHERE agent_code = ?
                """, agentCode);
    }

    public int markStaleOffline(int secondsThreshold) {
        return jdbcTemplate.update("""
                UPDATE agent_instance
                SET instance_status = 'OFFLINE'
                WHERE instance_status = 'ONLINE'
                  AND last_heartbeat_at IS NOT NULL
                  AND last_heartbeat_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? SECOND)
                """, secondsThreshold);
    }
}
