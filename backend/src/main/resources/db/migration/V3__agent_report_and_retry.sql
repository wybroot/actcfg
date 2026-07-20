CREATE TABLE agent_execution_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_code VARCHAR(64) NOT NULL UNIQUE,
  task_id BIGINT NOT NULL,
  package_build_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  environment_id BIGINT NOT NULL,
  execution_host VARCHAR(128) NULL,
  execution_status VARCHAR(32) NOT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  failed_step VARCHAR(64) NULL,
  failure_reason VARCHAR(1024) NULL,
  health_check_result VARCHAR(512) NULL,
  report_content TEXT NULL,
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_report_task (task_id)
);

CREATE TABLE agent_retry_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  retry_no INT NOT NULL,
  failed_step VARCHAR(64) NULL,
  failure_reason VARCHAR(1024) NULL,
  triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  result_status VARCHAR(32) NULL,
  KEY idx_retry_task (task_id, retry_no)
);
