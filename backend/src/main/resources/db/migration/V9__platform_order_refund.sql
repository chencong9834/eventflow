INSERT INTO sys_role_template_permission (template_id, permission_id) VALUES
  (3101, 4006),
  (3101, 4008);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
INNER JOIN sys_permission p ON p.id IN (4006, 4008)
WHERE r.code = 'PLATFORM'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
