-- 部署包生命周期：交付后状态流转（ACTIVE→ARCHIVED→DEPRECATED→PURGED）、下载计数与保留期
ALTER TABLE package_build ADD COLUMN lifecycle_state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE package_build ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE package_build ADD COLUMN last_downloaded_at DATETIME NULL;
ALTER TABLE package_build ADD COLUMN retention_until DATETIME NULL;

-- 存量成功包默认活跃、保留期从创建时间起顺延 90 天
UPDATE package_build
SET retention_until = TIMESTAMPADD(DAY, 90, created_at)
WHERE retention_until IS NULL;

-- 生命周期状态检索（清理任务按 lifecycle_state + retention_until 扫描）
CREATE INDEX idx_package_lifecycle ON package_build (lifecycle_state, retention_until);
