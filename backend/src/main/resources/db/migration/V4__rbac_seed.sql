-- RBAC 角色初始化
INSERT INTO sys_role (role_code, role_name, status) VALUES
('SUPER_ADMIN',    '超级管理员', 'ENABLED'),
('OPS',            '运维人员',   'ENABLED'),
('IMPL_ENGINEER',  '实施工程师', 'ENABLED'),
('AUDITOR',        '审计人员',   'ENABLED');

-- 初始用户（密码均为 Admin@123，BCrypt 加密）
-- admin    -> SUPER_ADMIN
-- ops      -> OPS
-- impl     -> IMPL_ENGINEER
-- auditor  -> AUDITOR
INSERT INTO sys_user (username, display_name, password_hash, status) VALUES
('admin',   '超级管理员', '$2a$10$7QwOX6HIjNp7qgzpVE1XeuvhGVBW4GjmTD4JUwi./hYJjnEGH6l4S', 'ENABLED'),
('ops',     '运维人员',   '$2a$10$7QwOX6HIjNp7qgzpVE1XeuvhGVBW4GjmTD4JUwi./hYJjnEGH6l4S', 'ENABLED'),
('impl',    '实施工程师', '$2a$10$7QwOX6HIjNp7qgzpVE1XeuvhGVBW4GjmTD4JUwi./hYJjnEGH6l4S', 'ENABLED'),
('auditor', '审计人员',   '$2a$10$7QwOX6HIjNp7qgzpVE1XeuvhGVBW4GjmTD4JUwi./hYJjnEGH6l4S', 'ENABLED');

-- 用户角色绑定
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE (u.username = 'admin'   AND r.role_code = 'SUPER_ADMIN')
   OR (u.username = 'ops'     AND r.role_code = 'OPS')
   OR (u.username = 'impl'    AND r.role_code = 'IMPL_ENGINEER')
   OR (u.username = 'auditor' AND r.role_code = 'AUDITOR');
