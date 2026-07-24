-- RBAC 角色初始化
INSERT INTO sys_role (role_code, role_name, status) VALUES
('SUPER_ADMIN',    '超级管理员', 'ENABLED'),
('OPS',            '运维人员',   'ENABLED'),
('IMPL_ENGINEER',  '实施工程师', 'ENABLED'),
('AUDITOR',        '审计人员',   'ENABLED');

-- 初始用户（密码均为 Admin@123，BCrypt $2a$10$ 加密）
-- 注意：hash 已通过 BCryptPasswordEncoder.matches 验证，确认对应 Admin@123
-- admin    -> SUPER_ADMIN
-- ops      -> OPS
-- impl     -> IMPL_ENGINEER
-- auditor  -> AUDITOR
INSERT INTO sys_user (username, display_name, password_hash, status) VALUES
('admin',   '超级管理员', '$2a$10$vVdNjsbkolsexq8EtXBG2enGD4lq5uuoZ8OTANc/OotENeta/UBZK', 'ENABLED'),
('ops',     '运维人员',   '$2a$10$vVdNjsbkolsexq8EtXBG2enGD4lq5uuoZ8OTANc/OotENeta/UBZK', 'ENABLED'),
('impl',    '实施工程师', '$2a$10$vVdNjsbkolsexq8EtXBG2enGD4lq5uuoZ8OTANc/OotENeta/UBZK', 'ENABLED'),
('auditor', '审计人员',   '$2a$10$vVdNjsbkolsexq8EtXBG2enGD4lq5uuoZ8OTANc/OotENeta/UBZK', 'ENABLED');

-- 用户角色绑定
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE (u.username = 'admin'   AND r.role_code = 'SUPER_ADMIN')
   OR (u.username = 'ops'     AND r.role_code = 'OPS')
   OR (u.username = 'impl'    AND r.role_code = 'IMPL_ENGINEER')
   OR (u.username = 'auditor' AND r.role_code = 'AUDITOR');
