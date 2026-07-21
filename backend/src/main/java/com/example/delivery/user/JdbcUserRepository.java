package com.example.delivery.user;

import com.example.delivery.common.jdbc.JdbcMapperSupport;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC 版 UserRepository，local profile（有 DataSource）下使用。
 */
@Profile("local")
@Component
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- RowMapper 方法（而非字段，避免 final 字段初始化顺序问题）----

    private RoleEntity mapRole(ResultSet rs, int i) throws SQLException {
        return new RoleEntity(
                rs.getLong("id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("status")
        );
    }

    private UserEntity mapUser(ResultSet rs, int i) throws SQLException {
        long uid = rs.getLong("id");
        List<String> roles = jdbc.queryForList(
                "SELECT r.role_code FROM sys_role r " +
                "JOIN sys_user_role ur ON ur.role_id = r.id " +
                "WHERE ur.user_id = ? AND r.status = 'ENABLED'", String.class, uid);
        return new UserEntity(
                uid,
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getString("status"),
                JdbcMapperSupport.nullableDateTime(rs, "created_at"),
                JdbcMapperSupport.nullableDateTime(rs, "updated_at"),
                roles
        );
    }

    // ---- UserRepository ----

    @Override
    public List<UserEntity> findAll() {
        return jdbc.query(
                "SELECT * FROM sys_user WHERE status = 'ENABLED' ORDER BY id", this::mapUser);
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        List<UserEntity> list = jdbc.query(
                "SELECT * FROM sys_user WHERE id = ? AND status = 'ENABLED'", this::mapUser, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        List<UserEntity> list = jdbc.query(
                "SELECT * FROM sys_user WHERE username = ? AND status = 'ENABLED'", this::mapUser, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public UserEntity save(UserEntity user) {
        if (user.id() == null) {
            GeneratedKeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO sys_user (username, display_name, password_hash, status) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.username());
                ps.setString(2, user.displayName());
                ps.setString(3, user.passwordHash());
                ps.setString(4, user.status() != null ? user.status() : "ENABLED");
                return ps;
            }, kh);
            long newId = Objects.requireNonNull(kh.getKey()).longValue();
            return findById(newId).orElseThrow();
        } else {
            jdbc.update("UPDATE sys_user SET display_name=?, updated_at=NOW() WHERE id=?",
                    user.displayName(), user.id());
            return findById(user.id()).orElseThrow();
        }
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update("UPDATE sys_user SET status='DISABLED', updated_at=NOW() WHERE id=?", id);
    }

    @Override
    public void updateRoles(Long userId, List<Long> roleIds) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id=?", userId);
        if (!roleIds.isEmpty()) {
            List<Object[]> batch = new ArrayList<>();
            roleIds.forEach(rid -> batch.add(new Object[]{userId, rid}));
            jdbc.batchUpdate("INSERT INTO sys_user_role (user_id, role_id) VALUES (?,?)", batch);
        }
    }

    @Override
    public List<RoleEntity> findAllRoles() {
        return jdbc.query(
                "SELECT * FROM sys_role WHERE status='ENABLED' ORDER BY id", this::mapRole);
    }
}
