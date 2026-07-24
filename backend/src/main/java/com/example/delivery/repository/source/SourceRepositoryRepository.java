package com.example.delivery.repository.source;

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
public class SourceRepositoryRepository {
    private static final String COLUMNS =
            "id, repo_code, repo_name, repo_type, base_url, username, password_enc, description, status, created_at, updated_at, deleted";

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SourceRepositoryEntity> mapper = (rs, rowNum) -> new SourceRepositoryEntity(
            rs.getLong("id"),
            rs.getString("repo_code"),
            rs.getString("repo_name"),
            SourceRepositoryType.valueOf(rs.getString("repo_type")),
            rs.getString("base_url"),
            rs.getString("username"),
            rs.getString("password_enc"),
            rs.getString("description"),
            rs.getString("status"),
            JdbcMapperSupport.nullableDateTime(rs, "created_at"),
            JdbcMapperSupport.nullableDateTime(rs, "updated_at"),
            rs.getBoolean("deleted")
    );

    public SourceRepositoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SourceRepositoryEntity> findAllActive() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM source_repository WHERE deleted = 0 ORDER BY created_at DESC, id DESC",
                mapper);
    }

    public Optional<SourceRepositoryEntity> findActiveById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT " + COLUMNS + " FROM source_repository WHERE id = ? AND deleted = 0",
                    mapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByCode(String repoCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM source_repository WHERE repo_code = ? AND deleted = 0",
                Integer.class, repoCode);
        return count != null && count > 0;
    }

    public SourceRepositoryEntity insert(CreateSourceRepositoryRequest req, String passwordEnc, String status) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO source_repository (repo_code, repo_name, repo_type, base_url, username, password_enc, description, status, created_at, updated_at, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.repoCode());
            ps.setString(2, req.repoName());
            ps.setString(3, req.repoType() == null ? SourceRepositoryType.HARBOR.name() : req.repoType().name());
            ps.setString(4, req.baseUrl());
            ps.setString(5, req.username());
            ps.setString(6, passwordEnc);
            ps.setString(7, req.description());
            ps.setString(8, status);
            return ps;
        }, kh);
        return findActiveById(kh.getKey().longValue()).orElseThrow();
    }

    public SourceRepositoryEntity update(Long id, UpdateSourceRepositoryRequest req, String passwordEnc, String status) {
        jdbcTemplate.update("""
                UPDATE source_repository
                SET repo_name = ?, repo_type = ?, base_url = ?, username = ?, password_enc = ?, description = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = 0
                """,
                req.repoName(),
                req.repoType() == null ? SourceRepositoryType.HARBOR.name() : req.repoType().name(),
                req.baseUrl(),
                req.username(),
                passwordEnc,
                req.description(),
                status,
                id);
        return findActiveById(id).orElseThrow();
    }

    public void softDelete(Long id) {
        jdbcTemplate.update(
                "UPDATE source_repository SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0",
                id);
    }
}
