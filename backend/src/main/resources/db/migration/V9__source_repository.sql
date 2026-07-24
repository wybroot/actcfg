-- 源仓库：前端可管理的镜像/制品来源（当前支持 HARBOR 类型），只同步元数据，不搬运镜像层。
CREATE TABLE source_repository (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repo_code VARCHAR(64) NOT NULL UNIQUE,
  repo_name VARCHAR(128) NOT NULL,
  repo_type VARCHAR(32) NOT NULL DEFAULT 'HARBOR',
  base_url VARCHAR(512) NOT NULL,
  username VARCHAR(128) NULL,
  -- 密码密文（AES-256-GCM，enc:keyId:iv:ct 前缀），复用敏感配置加密密钥
  password_enc VARCHAR(1024) NULL,
  description VARCHAR(512) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0
);

-- 示例源仓库（默认停用，仅用于让下拉非空，凭证需自行填写）
INSERT INTO source_repository (repo_code, repo_name, repo_type, base_url, username, description, status)
VALUES ('harbor-example', '示例 Harbor', 'HARBOR', 'https://harbor.example.com', 'admin', '示例配置，启用前请填写真实地址与凭证', 'DISABLED');
