-- 主办方租户内置多角色：管理员 / 活动运营 / 现场核销 / 财务
UPDATE sys_role_template
SET code = 'ORGANIZER_ADMIN', name = '主办方管理员'
WHERE id = 3102;

INSERT INTO sys_role_template (id, tenant_type, code, name, created_by) VALUES
  (3104, 'ORGANIZER', 'ORGANIZER_OPERATOR', '活动运营', 0),
  (3105, 'ORGANIZER', 'ORGANIZER_CHECKIN', '现场核销', 0),
  (3106, 'ORGANIZER', 'ORGANIZER_FINANCE', '财务', 0);

INSERT INTO sys_role_template_permission (template_id, permission_id) VALUES
  (3104, 4003),
  (3104, 4004),
  (3104, 4005),
  (3104, 4006),
  (3105, 4006),
  (3105, 4007),
  (3106, 4003),
  (3106, 4006),
  (3106, 4008);

UPDATE sys_role
SET code = 'ORGANIZER_ADMIN', name = '主办方管理员'
WHERE code = 'ORGANIZER';

INSERT INTO sys_role (id, tenant_id, code, name, created_by)
SELECT UUID_SHORT(), t.id, tpl.code, tpl.name, 0
FROM sys_tenant t
INNER JOIN sys_role_template tpl ON tpl.tenant_type = 'ORGANIZER'
WHERE t.type = 'ORGANIZER'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role r WHERE r.tenant_id = t.id AND r.code = tpl.code
  );

INSERT INTO sys_role_permission (role_id, permission_id, tenant_id)
SELECT r.id, tp.permission_id, r.tenant_id
FROM sys_role r
INNER JOIN sys_role_template tpl
  ON tpl.tenant_type = 'ORGANIZER' AND tpl.code = r.code
INNER JOIN sys_role_template_permission tp ON tp.template_id = tpl.id
LEFT JOIN sys_role_permission existing
  ON existing.role_id = r.id AND existing.permission_id = tp.permission_id
WHERE existing.role_id IS NULL;
