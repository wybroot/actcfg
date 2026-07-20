INSERT INTO repo_resource (resource_code, resource_name, resource_type, source_type, description, status, deleted)
SELECT 'RES-APP-001', '示例应用服务', 'JAR', 'UPLOAD', 'MVP 示例资源', 'ENABLED', 0
WHERE NOT EXISTS (SELECT 1 FROM repo_resource WHERE resource_code = 'RES-APP-001');

INSERT INTO repo_resource_version (resource_id, version, external_url, checksum, release_note, status)
SELECT r.id, '1.0.0', 'internal://repo/example-app-1.0.0.jar', 'sha256-placeholder', '初始版本', 'ENABLED'
FROM repo_resource r
WHERE r.resource_code = 'RES-APP-001'
  AND NOT EXISTS (SELECT 1 FROM repo_resource_version v WHERE v.resource_id = r.id AND v.version = '1.0.0');

INSERT INTO deploy_plan (plan_code, plan_name, description, status, deleted)
SELECT 'PLAN-001', '标准单机部署方案', 'MVP 示例部署方案', 'ENABLED', 0
WHERE NOT EXISTS (SELECT 1 FROM deploy_plan WHERE plan_code = 'PLAN-001');

INSERT INTO deploy_plan_version (plan_id, version, status, published_at)
SELECT p.id, '1.0.0', 'PUBLISHED', CURRENT_TIMESTAMP
FROM deploy_plan p
WHERE p.plan_code = 'PLAN-001'
  AND NOT EXISTS (SELECT 1 FROM deploy_plan_version v WHERE v.plan_id = p.id AND v.version = '1.0.0');

UPDATE deploy_plan p
JOIN deploy_plan_version v ON v.plan_id = p.id AND v.version = '1.0.0'
SET p.current_version_id = v.id
WHERE p.plan_code = 'PLAN-001' AND p.current_version_id IS NULL;

INSERT INTO deploy_component (plan_version_id, component_name, component_type, resource_version_id, deploy_order, config_template, health_check)
SELECT pv.id, '应用服务', 'APP', rv.id, 1, 'server.port=8080', 'GET /actuator/health'
FROM deploy_plan p
JOIN deploy_plan_version pv ON pv.plan_id = p.id AND pv.version = '1.0.0'
JOIN repo_resource r ON r.resource_code = 'RES-APP-001'
JOIN repo_resource_version rv ON rv.resource_id = r.id AND rv.version = '1.0.0'
WHERE p.plan_code = 'PLAN-001'
  AND NOT EXISTS (SELECT 1 FROM deploy_component c WHERE c.plan_version_id = pv.id AND c.component_name = '应用服务');

INSERT INTO customer (customer_code, customer_name, short_name, industry, status, deleted)
SELECT 'CUST001', '示例客户', '示例', '政企', 'ENABLED', 0
WHERE NOT EXISTS (SELECT 1 FROM customer WHERE customer_code = 'CUST001');

INSERT INTO customer_environment (customer_id, environment_name, environment_type, deploy_plan_version_id, network_desc, status)
SELECT c.id, '生产环境', 'PROD', pv.id, 'MVP 示例网络', 'ENABLED'
FROM customer c
JOIN deploy_plan p ON p.plan_code = 'PLAN-001'
JOIN deploy_plan_version pv ON pv.plan_id = p.id AND pv.version = '1.0.0'
WHERE c.customer_code = 'CUST001'
  AND NOT EXISTS (SELECT 1 FROM customer_environment e WHERE e.customer_id = c.id AND e.environment_type = 'PROD');

INSERT INTO env_variable (environment_id, variable_key, variable_value, masked_value, is_sensitive)
SELECT e.id, 'db.password', NULL, '******', 1
FROM customer_environment e
JOIN customer c ON c.id = e.customer_id
WHERE c.customer_code = 'CUST001' AND e.environment_type = 'PROD'
  AND NOT EXISTS (SELECT 1 FROM env_variable v WHERE v.environment_id = e.id AND v.variable_key = 'db.password');

INSERT INTO package_build (package_code, customer_id, environment_id, deploy_plan_version_id, package_version, build_status, immutable, file_path, checksum, build_log)
SELECT 'PKG202607170001', c.id, e.id, e.deploy_plan_version_id, '1.0.0', 'SUCCESS', 1, 'packages/PKG202607170001.zip', 'sha256-placeholder', 'MVP 示例部署包'
FROM customer c
JOIN customer_environment e ON e.customer_id = c.id AND e.environment_type = 'PROD'
WHERE c.customer_code = 'CUST001'
  AND NOT EXISTS (SELECT 1 FROM package_build p WHERE p.package_code = 'PKG202607170001');

INSERT INTO package_manifest (package_build_id, manifest_json, checksum_file_path)
SELECT p.id,
       CONCAT('{"packageCode":"', p.package_code, '","environment":"PROD","steps":["CHECK_ENV","DEPLOY","HEALTH_CHECK"]}'),
       'packages/PKG202607170001/checksum.sha256'
FROM package_build p
WHERE p.package_code = 'PKG202607170001'
  AND NOT EXISTS (SELECT 1 FROM package_manifest m WHERE m.package_build_id = p.id);

INSERT INTO agent_task (task_code, package_build_id, task_type, task_status, started_at, finished_at, result_summary)
SELECT 'TASK-0001', p.id, 'OFFLINE_DEPLOY', 'SUCCESS', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MINUTE), CURRENT_TIMESTAMP, '离线部署示例任务'
FROM package_build p
WHERE p.package_code = 'PKG202607170001'
  AND NOT EXISTS (SELECT 1 FROM agent_task t WHERE t.task_code = 'TASK-0001');

INSERT INTO agent_execution_log (task_id, step_code, step_name, step_status, log_level, log_content, retry_count, started_at, finished_at)
SELECT t.id, 'CHECK_ENV', '环境检测', 'SUCCESS', 'INFO', 'environment check passed', 0, t.started_at, t.started_at
FROM agent_task t
WHERE t.task_code = 'TASK-0001'
  AND NOT EXISTS (SELECT 1 FROM agent_execution_log l WHERE l.task_id = t.id AND l.step_code = 'CHECK_ENV');

INSERT INTO agent_execution_log (task_id, step_code, step_name, step_status, log_level, log_content, retry_count, started_at, finished_at)
SELECT t.id, 'DEPLOY', '执行部署', 'SUCCESS', 'INFO', 'deploy finished', 0, t.started_at, t.finished_at
FROM agent_task t
WHERE t.task_code = 'TASK-0001'
  AND NOT EXISTS (SELECT 1 FROM agent_execution_log l WHERE l.task_id = t.id AND l.step_code = 'DEPLOY');
