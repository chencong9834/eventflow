INSERT INTO sys_permission (id, code, name, created_by) VALUES
  (4013, 'user:write', '维护本租户账号', 0);

INSERT INTO sys_role_template_permission (template_id, permission_id) VALUES
  (3102, 4013);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, 4013
FROM sys_role r
WHERE r.code = 'ORGANIZER_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = 4013
  );
