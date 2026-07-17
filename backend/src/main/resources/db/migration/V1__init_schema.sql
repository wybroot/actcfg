CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE repo_resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_code VARCHAR(64) NOT NULL UNIQUE,
  resource_name VARCHAR(128) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  description VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE repo_resource_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  version VARCHAR(64) NOT NULL,
  external_url VARCHAR(512) NULL,
  image_repository VARCHAR(256) NULL,
  image_tag VARCHAR(128) NULL,
  checksum VARCHAR(128) NULL,
  release_note VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_resource_version (resource_id, version)
);

CREATE TABLE deploy_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_code VARCHAR(64) NOT NULL UNIQUE,
  plan_name VARCHAR(128) NOT NULL,
  description VARCHAR(512) NULL,
  current_version_id BIGINT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE deploy_plan_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  version VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plan_version (plan_id, version)
);

CREATE TABLE deploy_component (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_version_id BIGINT NOT NULL,
  component_name VARCHAR(128) NOT NULL,
  component_type VARCHAR(32) NOT NULL,
  resource_version_id BIGINT NULL,
  deploy_order INT NOT NULL,
  config_template TEXT NULL,
  health_check VARCHAR(1024) NULL
);

CREATE TABLE customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_code VARCHAR(64) NOT NULL UNIQUE,
  customer_name VARCHAR(128) NOT NULL,
  short_name VARCHAR(64) NULL,
  industry VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE customer_environment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  environment_name VARCHAR(128) NOT NULL,
  environment_type VARCHAR(32) NOT NULL,
  deploy_plan_version_id BIGINT NULL,
  network_desc VARCHAR(1024) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer_env (customer_id, environment_type)
);

CREATE TABLE env_variable (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  environment_id BIGINT NOT NULL,
  variable_key VARCHAR(128) NOT NULL,
  variable_value TEXT NULL,
  masked_value VARCHAR(256) NULL,
  sensitive TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_env_variable (environment_id, variable_key)
);

CREATE TABLE package_build (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_code VARCHAR(64) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  environment_id BIGINT NOT NULL,
  deploy_plan_version_id BIGINT NOT NULL,
  package_version VARCHAR(64) NOT NULL,
  build_status VARCHAR(32) NOT NULL DEFAULT 'BUILDING',
  immutable TINYINT NOT NULL DEFAULT 0,
  file_path VARCHAR(512) NULL,
  checksum VARCHAR(128) NULL,
  build_log TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_package_customer_env (customer_id, environment_id, created_at)
);

CREATE TABLE package_manifest (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_build_id BIGINT NOT NULL UNIQUE,
  manifest_json LONGTEXT NOT NULL,
  checksum_file_path VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE agent_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_code VARCHAR(64) NOT NULL UNIQUE,
  package_build_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  result_summary VARCHAR(1024) NULL,
  KEY idx_agent_task_status (task_status, started_at)
);

CREATE TABLE agent_execution_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  step_code VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  step_status VARCHAR(32) NOT NULL,
  log_level VARCHAR(16) NOT NULL DEFAULT 'INFO',
  log_content TEXT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  KEY idx_agent_log_task (task_id, step_code)
);

CREATE TABLE audit_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NULL,
  operator_name VARCHAR(128) NULL,
  module VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  result VARCHAR(32) NOT NULL,
  ip_address VARCHAR(64) NULL,
  parameter_summary VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_operator_time (operator_id, created_at)
);

CREATE TABLE audit_download_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  downloader_id BIGINT NULL,
  downloader_name VARCHAR(128) NULL,
  target_type VARCHAR(64) NOT NULL,
  target_name VARCHAR(256) NOT NULL,
  customer_id BIGINT NULL,
  environment_id BIGINT NULL,
  file_size BIGINT NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
