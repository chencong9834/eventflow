ALTER TABLE sys_role_permission DROP FOREIGN KEY fk_srp_tenant;
ALTER TABLE sys_role_permission DROP COLUMN tenant_id;
