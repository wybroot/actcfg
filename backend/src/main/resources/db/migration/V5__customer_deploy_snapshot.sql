-- 客户配置快照：客户绑定部署方案版本时，深拷贝方案+组件为独立副本，后续编辑与源方案解耦
CREATE TABLE customer_deploy_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  environment_id BIGINT NOT NULL,
  source_plan_version_id BIGINT NOT NULL,
  plan_name VARCHAR(128) NOT NULL,
  version_label VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_snapshot_env (environment_id)
);

CREATE TABLE customer_deploy_snapshot_component (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id BIGINT NOT NULL,
  component_name VARCHAR(128) NOT NULL,
  component_type VARCHAR(32) NOT NULL,
  resource_version_id BIGINT NULL,
  deploy_order INT NOT NULL,
  config_template TEXT NULL,
  health_check VARCHAR(1024) NULL,
  KEY idx_snapshot_component (snapshot_id)
);
