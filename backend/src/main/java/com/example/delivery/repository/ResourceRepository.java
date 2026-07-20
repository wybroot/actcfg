package com.example.delivery.repository;

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
public class ResourceRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ResourceEntity> resourceMapper = (resultSet, rowNum) -> new ResourceEntity(
            resultSet.getLong("id"),
            resultSet.getString("resource_code"),
            resultSet.getString("resource_name"),
            ResourceType.valueOf(resultSet.getString("resource_type")),
            ResourceSourceType.valueOf(resultSet.getString("source_type")),
            resultSet.getString("description"),
            resultSet.getString("status"),
            JdbcMapperSupport.nullableDateTime(resultSet, "created_at"),
            JdbcMapperSupport.nullableDateTime(resultSet, "updated_at"),
            resultSet.getBoolean("deleted")
    );

    private final RowMapper<ResourceVersionEntity> versionMapper = (resultSet, rowNum) -> new ResourceVersionEntity(
            resultSet.getLong("id"),
            resultSet.getLong("resource_id"),
            resultSet.getString("version"),
            resultSet.getString("external_url"),
            resultSet.getString("image_repository"),
            resultSet.getString("image_tag"),
            resultSet.getString("checksum"),
            resultSet.getString("release_note"),
            resultSet.getString("status"),
            JdbcMapperSupport.nullableDateTime(resultSet, "created_at")
    );

    public ResourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ResourceEntity> findAllActive() {
        return jdbcTemplate.query("""
                SELECT id, resource_code, resource_name, resource_type, source_type, description, status, created_at, updated_at, deleted
                FROM repo_resource
                WHERE deleted = 0
                ORDER BY created_at DESC, id DESC
                """, resourceMapper);
    }

    public Optional<ResourceEntity> findActiveById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, resource_code, resource_name, resource_type, source_type, description, status, created_at, updated_at, deleted
                    FROM repo_resource
                    WHERE id = ? AND deleted = 0
                    """, resourceMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsByCode(String resourceCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM repo_resource WHERE resource_code = ? AND deleted = 0",
                Integer.class,
                resourceCode
        );
        return count != null && count > 0;
    }

    public ResourceEntity insertResource(CreateResourceRequest request, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO repo_resource (resource_code, resource_name, resource_type, source_type, description, status, created_at, updated_at, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.resourceCode());
            statement.setString(2, request.resourceName());
            statement.setString(3, request.resourceType().name());
            statement.setString(4, request.sourceType().name());
            statement.setString(5, request.description());
            statement.setString(6, status);
            return statement;
        }, keyHolder);
        return findActiveById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public ResourceEntity updateResource(Long id, UpdateResourceRequest request) {
        jdbcTemplate.update("""
                UPDATE repo_resource
                SET resource_name = ?, resource_type = ?, source_type = ?, description = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = 0
                """,
                request.resourceName(),
                request.resourceType().name(),
                request.sourceType().name(),
                request.description(),
                request.status(),
                id
        );
        return findActiveById(id).orElseThrow();
    }

    public void softDelete(Long id) {
        jdbcTemplate.update("""
                UPDATE repo_resource
                SET deleted = 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted = 0
                """, id);
    }

    public List<ResourceVersionEntity> findVersionsByResourceId(Long resourceId) {
        return jdbcTemplate.query("""
                SELECT id, resource_id, version, external_url, image_repository, image_tag, checksum, release_note, status, created_at
                FROM repo_resource_version
                WHERE resource_id = ?
                ORDER BY created_at DESC, id DESC
                """, versionMapper, resourceId);
    }

    public Optional<ResourceVersionEntity> findVersionById(Long versionId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT id, resource_id, version, external_url, image_repository, image_tag, checksum, release_note, status, created_at
                    FROM repo_resource_version
                    WHERE id = ?
                    """, versionMapper, versionId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsVersion(Long resourceId, String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM repo_resource_version WHERE resource_id = ? AND version = ?",
                Integer.class,
                resourceId,
                version
        );
        return count != null && count > 0;
    }

    public ResourceVersionEntity insertVersion(Long resourceId, CreateResourceVersionRequest request, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO repo_resource_version (resource_id, version, external_url, image_repository, image_tag, checksum, release_note, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, resourceId);
            statement.setString(2, request.version());
            statement.setString(3, request.externalUrl());
            statement.setString(4, request.imageRepository());
            statement.setString(5, request.imageTag());
            statement.setString(6, request.checksum());
            statement.setString(7, request.releaseNote());
            statement.setString(8, status);
            return statement;
        }, keyHolder);
        return findVersionById(keyHolder.getKey().longValue()).orElseThrow();
    }
}
