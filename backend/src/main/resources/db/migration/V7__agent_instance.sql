-- 在线 Agent 实例：客户现场 agent 注册与心跳
CREATE TABLE agent_instance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_code VARCHAR(64) NOT NULL UNIQUE,
  hostname VARCHAR(128) NULL,
  ip_address VARCHAR(64) NULL,
  customer_id BIGINT NULL,
  environment_id BIGINT NULL,
  instance_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE',
  last_heartbeat_at DATETIME NULL,
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_agent_instance_status (instance_status, last_heartbeat_at)
);
