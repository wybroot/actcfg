package com.example.delivery.packagebuild;

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
public class PackageBuildRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_COLUMNS = "id, package_code, customer_id, environment_id, "
            + "deploy_plan_version_id, package_version, build_status, immutable, file_path, checksum, "
            + "created_at, lifecycle_state, download_count, last_downloaded_at, retention_until";

    private final RowMapper<PackageBuildEntity> packageMapper = (resultSet, rowNum) -> new PackageBuildEntity(
            resultSet.getLong("id"),
            resultSet.getString("package_code"),
            resultSet.getLong("customer_id"),
            resultSet.getLong("environment_id"),
            resultSet.getLong("deploy_plan_version_id"),
            resultSet.getString("package_version"),
            PackageBuildStatus.valueOf(resultSet.getString("build_status")),
            resultSet.getBoolean("immutable"),
            resultSet.getString("file_path"),
            resultSet.getString("checksum"),
            JdbcMapperSupport.nullableDateTime(resultSet, "created_at"),
            PackageLifecycleState.valueOf(resultSet.getString("lifecycle_state")),
            resultSet.getLong("download_count"),
            JdbcMapperSupport.nullableDateTime(resultSet, "last_downloaded_at"),
            JdbcMapperSupport.nullableDateTime(resultSet, "retention_until")
    );

    public PackageBuildRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PackageBuildEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM package_build ORDER BY created_at DESC, id DESC",
                packageMapper);
    }

    public Optional<PackageBuildEntity> findById(Long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT " + SELECT_COLUMNS + " FROM package_build WHERE id = ?",
                    packageMapper, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /** 更新生命周期状态。 */
    public void updateLifecycle(Long id, PackageLifecycleState state) {
        jdbcTemplate.update("UPDATE package_build SET lifecycle_state = ? WHERE id = ?", state.name(), id);
    }

    /** 记录一次下载：计数 +1 并刷新最后下载时间。 */
    public void incrementDownload(Long id) {
        jdbcTemplate.update("""
                UPDATE package_build
                SET download_count = download_count + 1, last_downloaded_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, id);
    }

    /** 查询可清理的包：已废弃且过保留期。 */
    public List<PackageBuildEntity> findPurgeable() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM package_build "
                        + "WHERE lifecycle_state = 'DEPRECATED' "
                        + "AND retention_until IS NOT NULL AND retention_until < CURRENT_TIMESTAMP",
                packageMapper);
    }

    public PackageBuildEntity insertPackage(String packageCode, CreatePackageBuildRequest request, String checksum, String filePath, int retentionDays) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO package_build (package_code, customer_id, environment_id, deploy_plan_version_id, package_version, build_status, immutable, file_path, checksum, created_at, lifecycle_state, download_count, retention_until)
                    VALUES (?, ?, ?, ?, ?, 'SUCCESS', 1, ?, ?, CURRENT_TIMESTAMP, 'ACTIVE', 0, TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP))
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, packageCode);
            statement.setLong(2, request.customerId());
            statement.setLong(3, request.environmentId());
            statement.setLong(4, request.deployPlanVersionId());
            statement.setString(5, request.packageVersion());
            statement.setString(6, filePath);
            statement.setString(7, checksum);
            statement.setInt(8, retentionDays);
            return statement;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public void updateArtifacts(Long id, String checksum, String filePath) {
        jdbcTemplate.update("""
                UPDATE package_build
                SET checksum = ?, file_path = ?
                WHERE id = ?
                """, checksum, filePath, id);
    }

    public void insertManifest(Long packageBuildId, String manifestJson, String checksumFilePath) {
        jdbcTemplate.update("""
                INSERT INTO package_manifest (package_build_id, manifest_json, checksum_file_path, created_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, packageBuildId, manifestJson, checksumFilePath);
    }

    public Optional<PackageManifest> findManifest(Long packageBuildId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT m.package_build_id, m.manifest_json, p.checksum
                    FROM package_manifest m
                    JOIN package_build p ON p.id = m.package_build_id
                    WHERE m.package_build_id = ?
                    """, (rs, rowNum) -> new PackageManifest(
                    rs.getLong("package_build_id"),
                    rs.getString("manifest_json"),
                    rs.getString("checksum")
            ), packageBuildId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM package_manifest WHERE package_build_id = ?", id);
        jdbcTemplate.update("DELETE FROM package_build WHERE id = ?", id);
    }
}
